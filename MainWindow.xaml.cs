using System;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Interop;
using Microsoft.Web.WebView2.Core;

namespace PhasmoCompanion;

/// <summary>
/// Fenêtre principale : le journal complet (page embarquée dans l'exe, affichée par WebView2).
/// Les minuteurs ont leur propre fenêtre flottante (TimerWindow), façon LiveSplit.
/// Les raccourcis sont globaux et se modifient depuis le bouton ⚙, sans recompiler.
/// </summary>
public partial class MainWindow : Window
{
    private const int WM_HOTKEY = 0x0312;
    private const int WM_GETMINMAXINFO = 0x0024;

    [StructLayout(LayoutKind.Sequential)] private struct POINT { public int X, Y; }
    [StructLayout(LayoutKind.Sequential)] private struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)]
    private struct MINMAXINFO { public POINT Reserve, MaxSize, MaxPosition, MinTrack, MaxTrack; }
    [StructLayout(LayoutKind.Sequential)]
    private struct MONITORINFO { public int Taille; public RECT Ecran, Travail; public int Drapeaux; }

    [DllImport("user32.dll")] private static extern IntPtr MonitorFromWindow(IntPtr hwnd, uint options);
    [DllImport("user32.dll")] private static extern bool GetMonitorInfo(IntPtr moniteur, ref MONITORINFO info);

    /// <summary>
    /// Sans bordure système, Windows agrandit la fenêtre à la taille de l'écran ENTIER : la barre
    /// de titre maison passe alors sous le bord haut de l'écran, et plus aucun bouton n'est
    /// cliquable. On impose donc la zone de travail (écran moins la barre des tâches).
    /// </summary>
    private static void LimiterALaZoneDeTravail(IntPtr hwnd, IntPtr lParam)
    {
        try
        {
            var moniteur = MonitorFromWindow(hwnd, 2);          // MONITOR_DEFAULTTONEAREST
            if (moniteur == IntPtr.Zero) return;

            var info = new MONITORINFO { Taille = Marshal.SizeOf<MONITORINFO>() };
            if (!GetMonitorInfo(moniteur, ref info)) return;

            var mmi = Marshal.PtrToStructure<MINMAXINFO>(lParam);
            mmi.MaxPosition.X = info.Travail.Left - info.Ecran.Left;
            mmi.MaxPosition.Y = info.Travail.Top - info.Ecran.Top;
            mmi.MaxSize.X = info.Travail.Right - info.Travail.Left;
            mmi.MaxSize.Y = info.Travail.Bottom - info.Travail.Top;
            mmi.MinTrack.X = 320;
            mmi.MinTrack.Y = 180;
            Marshal.StructureToPtr(mmi, lParam, true);
        }
        catch { }
    }

    private readonly TimerWindow _timers = new();
    private readonly HotkeyManager _hotkeys = new();
    private readonly MouseHook _mouse = new();
    private readonly SignatureHook _signature = new();
    private bool _ready;

    /// <summary>
    /// Faux tant que la fenêtre se construit. Pendant la construction, WPF déclenche déjà
    /// les événements des curseurs et de la liste d'opacité (une valeur ramenée dans ses
    /// bornes compte comme un changement) : sans ce garde-fou, ces événements écrasaient
    /// les réglages tout juste relus sur le disque, puis les réenregistraient par-dessus.
    /// </summary>
    private bool _uiReady;

    private static readonly string SettingsPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion", "window.txt");

    public MainWindow()
    {
        InitializeComponent();
        LoadSettings();
        _uiReady = true;
        _mouse.ButtonPressed += OnMouseButton;
        _signature.Declenche += OuvrirSignature;
        Loaded += async (_, _) =>
        {
            if (_timers.EtaitVisible) _timers.Show();
            BtnTimers.Opacity = _timers.IsVisible ? 1.0 : 0.45;
            await InitWeb();
        };
        Closed += (_, _) => { _timers.SaveSettings(); _timers.Close(); _signature.Dispose(); };
        Closing += (_, _) => SaveSettings();
    }

    // ---------- page du journal ----------
    private async Task InitWeb()
    {
        var userData = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PhasmoCompanion", "WebView2");
        Directory.CreateDirectory(userData);

        try
        {
            var env = await CoreWebView2Environment.CreateAsync(null, userData);
            await Web.EnsureCoreWebView2Async(env);
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "WebView2 n'a pas pu démarrer.\n\nInstallez « Microsoft Edge WebView2 Runtime » " +
                "(gratuit, chez Microsoft), puis relancez.\n\nDétail : " + ex.Message,
                "Journal Phasmo", MessageBoxButton.OK, MessageBoxImage.Warning);
            return;
        }

        var s = Web.CoreWebView2.Settings;
        s.AreDefaultContextMenusEnabled = false;
        s.IsStatusBarEnabled = false;
        s.AreDevToolsEnabled = true;          // F12 si jamais quelque chose cloche

        // la page envoie ses touches : elles deviennent les raccourcis globaux des minuteurs
        Web.CoreWebView2.WebMessageReceived += (_, args) =>
        {
            try { ApplyKeysFromPage(args.TryGetWebMessageAsString()); } catch { }
        };

        Web.CoreWebView2.NavigationCompleted += (_, _) =>
        {
            _ready = true;
            // le journal adopte les raccourcis enregistrés s'il n'a encore rien retenu de son côté
            Js("phasmo.setKeys(" + KeysJson() + ")");
            Js("phasmo.desktop()");           // masque les minuteurs de la page : ils ont leur fenêtre
        };

        // La page est écrite sur le disque et servie par un vrai nom d'hôte : sans cela
        // (NavigateToString) le navigateur embarqué refuse le stockage local, et TOUT serait
        // oublié à chaque lancement — touches, difficulté, favoris, enquête en cours.
        try
        {
            Directory.CreateDirectory(PageDir);
            File.WriteAllText(Path.Combine(PageDir, "index.html"), LoadPage(), new UTF8Encoding(false));
            Web.CoreWebView2.SetVirtualHostNameToFolderMapping(
                "journal.phasmo", PageDir, CoreWebView2HostResourceAccessKind.Allow);
            Web.CoreWebView2.Navigate("https://journal.phasmo/index.html");
        }
        catch
        {
            Web.CoreWebView2.NavigateToString(LoadPage());   // secours : rien ne sera retenu
        }
    }

    private static readonly string PageDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion", "page");

    /// <summary>Les raccourcis enregistrés, dans le vocabulaire de la page.</summary>
    private string KeysJson()
    {
        string K(HkAction a) => JsonEncodedText.Encode(FromVirtualKey(_hotkeys.Map[a].Vk)).ToString();
        return "{\"smudge\":\"" + K(HkAction.Smudge) + "\",\"hunt\":\"" + K(HkAction.Hunt) +
               "\",\"cd\":\"" + K(HkAction.Cooldown) + "\",\"tap\":\"" + K(HkAction.Tap) +
               "\",\"reset\":\"" + K(HkAction.TapReset) + "\"}";
    }

    /// <summary>Code Windows → code de touche tel que la page l'écrit (inverse de ToVirtualKey).</summary>
    private static string FromVirtualKey(uint vk)
    {
        if (vk == MouseHook.MOUSE_MIDDLE) return "mouse1";
        if (vk == MouseHook.MOUSE_X1) return "mouse3";
        if (vk == MouseHook.MOUSE_X2) return "mouse4";
        if (vk >= 0x70 && vk <= 0x7B) return "f" + (vk - 0x70 + 1);
        if (vk >= 0x60 && vk <= 0x69) return "num" + (char)('0' + (vk - 0x60));
        switch (vk)
        {
            case 0x6B: return "num+";
            case 0x6D: return "num-";
            case 0x6A: return "num*";
            case 0x6F: return "num/";
            case 0x6E: return "num.";
        }
        if (vk >= 'A' && vk <= 'Z') return ((char)('a' + (vk - 'A'))).ToString();
        if (vk >= '0' && vk <= '9') return ((char)vk).ToString();
        return "";
    }

    /// <summary>
    /// Le journal est embarqué dans l'exe. Un phasmo.html posé à côté de l'exe le remplace,
    /// ce qui permet de mettre les données à jour sans recompiler.
    /// </summary>
    private string LoadPage()
    {
        var external = Path.Combine(AppContext.BaseDirectory, "phasmo.html");
        try
        {
            if (File.Exists(external))
            {
                var txt = File.ReadAllText(external);
                if (txt.Length > 20000) return txt;     // fichier manifestement complet
            }
        }
        catch { }

        var asm = Assembly.GetExecutingAssembly();
        foreach (var name in asm.GetManifestResourceNames())
        {
            if (!name.EndsWith("phasmo.html", StringComparison.OrdinalIgnoreCase)) continue;
            using var st = asm.GetManifestResourceStream(name)!;
            using var rd = new StreamReader(st);
            return rd.ReadToEnd();
        }

        return "<html><body style='font-family:sans-serif;background:#0E1116;color:#E3E6EC;padding:24px'>" +
               "<h2>Page introuvable</h2><p>La ressource phasmo.html est absente de l'exécutable.</p></body></html>";
    }

    /// <summary>
    /// Les touches réglées dans le panneau « Raccourcis clavier » du journal pilotent
    /// directement les minuteurs, même quand Phasmophobia a le focus.
    /// </summary>
    private void ApplyKeysFromPage(string? json)
    {
        if (string.IsNullOrWhiteSpace(json)) return;

        using var doc = JsonDocument.Parse(json);
        var root = doc.RootElement;
        if (!root.TryGetProperty("type", out var type)) return;

        // la difficulté choisie dans le journal pilote la grâce et les durées affichées
        if (type.GetString() == "diff")
        {
            var label = root.TryGetProperty("label", out var l) ? l.GetString() ?? "" : "";
            var grace = root.TryGetProperty("grace", out var g) && g.TryGetInt32(out var gi) ? gi : 3;
            int small = 30, med = 50, large = 60;
            if (root.TryGetProperty("hunt", out var h) && h.ValueKind == JsonValueKind.Array && h.GetArrayLength() >= 3)
            {
                var a = h.EnumerateArray().ToArray();
                if (a[0].TryGetInt32(out var v0)) small = v0;
                if (a[1].TryGetInt32(out var v1)) med = v1;
                if (a[2].TryGetInt32(out var v2)) large = v2;
            }
            var size = root.TryGetProperty("size", out var sz) && sz.TryGetInt32(out var si) ? si : -1;
            _timers.SetDifficulty(label, grace, small, med, large, size);
            return;
        }

        if (type.GetString() != "keys") return;
        if (!root.TryGetProperty("keys", out var keys)) return;

        var changed = false;
        changed |= Apply("smudge", HkAction.Smudge);
        changed |= Apply("hunt", HkAction.Hunt);
        changed |= Apply("cd", HkAction.Cooldown);
        changed |= Apply("tap", HkAction.Tap);
        changed |= Apply("reset", HkAction.TapReset);

        if (!changed) return;
        _hotkeys.Save();
        RegisterHotkeys();

        bool Apply(string field, HkAction action)
        {
            if (!keys.TryGetProperty(field, out var el)) return false;
            var vk = ToVirtualKey(el.GetString());
            if (vk == 0) return false;

            // les remises à zéro des minuteurs suivent : Maj + la même touche
            if (action is HkAction.Smudge or HkAction.Hunt or HkAction.Cooldown)
            {
                var reset = action switch
                {
                    HkAction.Smudge => HkAction.ResetSmudge,
                    HkAction.Hunt => HkAction.ResetHunt,
                    _ => HkAction.ResetCooldown
                };
                _hotkeys.Map[reset] = new Hotkey(4, vk);
            }

            if (_hotkeys.Map[action].Vk == vk && _hotkeys.Map[action].Mods == (action == HkAction.TapReset ? 4u : 0u))
                return false;

            _hotkeys.Map[action] = new Hotkey(action == HkAction.TapReset ? 4u : 0u, vk);
            return true;
        }
    }

    private static uint ToVirtualKey(string? k)
    {
        if (string.IsNullOrEmpty(k)) return 0;
        k = k.Trim().ToLowerInvariant();

        // boutons de souris envoyés par la page : mouse1 = molette, mouse3 / mouse4 = boutons latéraux
        if (k == "mouse1") return MouseHook.MOUSE_MIDDLE;
        if (k == "mouse3") return MouseHook.MOUSE_X1;
        if (k == "mouse4") return MouseHook.MOUSE_X2;

        // pavé numérique
        if (k.StartsWith("num"))
        {
            var rest = k[3..];
            if (rest.Length == 1 && rest[0] is >= '0' and <= '9') return (uint)(0x60 + (rest[0] - '0'));
            return rest switch
            {
                "+" => 0x6B, "-" => 0x6D, "*" => 0x6A, "/" => 0x6F, "." => 0x6E,
                "entree" => 0x0D,
                _ => 0
            };
        }

        if (k.Length >= 2 && k[0] == 'f' && int.TryParse(k[1..], out var n) && n is >= 1 and <= 12)
            return (uint)(0x70 + n - 1);                 // F1 à F12

        if (k.Length != 1) return 0;
        var c = char.ToUpperInvariant(k[0]);
        if (c is >= 'A' and <= 'Z') return c;            // lettres
        if (c is >= '0' and <= '9') return c;            // chiffres
        return 0;
    }

    private async void Js(string code)
    {
        if (!_ready || Web.CoreWebView2 is null) return;
        try { await Web.CoreWebView2.ExecuteScriptAsync(code); } catch { }
    }

    // ---------- raccourcis globaux ----------
    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        HwndSource.FromHwnd(new WindowInteropHelper(this).Handle)?.AddHook(Hook);
        RegisterHotkeys();
    }

    private void RegisterHotkeys()
    {
        var h = new WindowInteropHelper(this).Handle;
        if (h == IntPtr.Zero) return;
        _hotkeys.Unregister(h);
        var failed = _hotkeys.Register(h);
        _timers.ShowKeys(_hotkeys.Map[HkAction.Smudge].ToString(),
                         _hotkeys.Map[HkAction.Hunt].ToString(),
                         _hotkeys.Map[HkAction.Cooldown].ToString(),
                         _hotkeys.Map[HkAction.ResetAll].ToString(),
                         _hotkeys.Map[HkAction.ToggleTimers].ToString());
        Hint.Text = failed.Count == 0
            ? $"{_hotkeys.Map[HkAction.Smudge]} encens · {_hotkeys.Map[HkAction.Hunt]} chasse · " +
              $"{_hotkeys.Map[HkAction.Cooldown]} recharge · {_hotkeys.Map[HkAction.ResetAll]} remise à zéro · " +
              $"{_hotkeys.Map[HkAction.ToggleTimers]} minuteurs — ⚙ pour changer"
            : "Touches déjà prises par un autre logiciel : " + string.Join(", ", failed) + " — changez-les dans ⚙";
    }

    // ================= signature d'auteur (Ctrl + Alt + @) =================
    private bool _signatureOuverte;

    /// <summary>
    /// Deux codes à la suite, puis le certificat. Les fenêtres sont modales et enchaînées ;
    /// un code faux ou une annulation referme tout sans rien dire de plus.
    /// </summary>
    private void OuvrirSignature()
    {
        if (_signatureOuverte) return;
        _signatureOuverte = true;
        try
        {
            var un = new CodeDialog(this, "ÉTAPE 1 / 2", "Accès réservé",
                "Cette fenêtre est destinée à l'auteur de l'application. Saisissez le premier code.",
                PhasmoCompanion.Signature.PremierCode);
            if (un.ShowDialog() != true) return;

            var deux = new CodeDialog(this, "ÉTAPE 2 / 2", "Confirmation",
                "Premier code accepté. Saisissez le second code pour afficher le certificat.",
                PhasmoCompanion.Signature.SecondCode);
            if (deux.ShowDialog() != true) return;

            new CertificateWindow(this).ShowDialog();
        }
        catch (Exception ex)
        {
            MessageBox.Show("La signature n'a pas pu s'ouvrir.\n\n" + ex.Message,
                "Journal Phasmo", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
        finally { _signatureOuverte = false; }
    }

    /// <summary>Boutons de souris : capturés en réglages, sinon ils déclenchent l'action associée.</summary>
    private void OnMouseButton(uint vk)
    {
        if (_capturing is not null)
        {
            AssignCaptured(new Hotkey(0, vk));
            return;
        }
        if (Settings.Visibility == Visibility.Visible) return;

        foreach (var (action, _) in HotkeyManager.Actions)
            if (_hotkeys.Map[action].Vk == vk) { Run(action); return; }
    }

    private void Run(HkAction action)
    {
        switch (action)
        {
            case HkAction.Smudge: _timers.ToggleSmudge(); break;
            case HkAction.Hunt: _timers.ToggleHunt(); break;
            case HkAction.Cooldown: _timers.ToggleCd(); break;
            case HkAction.ResetAll: _timers.ResetAll(); break;
            case HkAction.ResetSmudge: _timers.ResetSmudge(); break;
            case HkAction.ResetHunt: _timers.ResetHunt(); break;
            case HkAction.ResetCooldown: _timers.ResetCd(); break;
            case HkAction.Tap: Js("phasmo.tap()"); break;
            case HkAction.TapReset: Js("phasmo.tapReset()"); break;
            case HkAction.ToggleTimers: ToggleTimers(); break;
            case HkAction.TogglePin: Topmost = !Topmost; UpdateTopButton(); SaveSettings(); break;
        }
    }

    private IntPtr Hook(IntPtr hwnd, int msg, IntPtr wParam, IntPtr lParam, ref bool handled)
    {
        if (msg == WM_GETMINMAXINFO) { LimiterALaZoneDeTravail(hwnd, lParam); return IntPtr.Zero; }
        if (msg != WM_HOTKEY) return IntPtr.Zero;
        var id = wParam.ToInt32();
        if (id < 100 || id > 199) return IntPtr.Zero;
        Run(HotkeyManager.FromId(id));
        handled = true;
        return IntPtr.Zero;
    }

    // ---------- boutons ----------
    private void ToggleTimers()
    {
        if (_timers.IsVisible) _timers.Hide();
        else { _timers.Show(); _timers.Activate(); }
        BtnTimers.Opacity = _timers.IsVisible ? 1.0 : 0.45;
        _timers.SaveSettings();          // afficher ou masquer est un choix, on le retient
    }

    private void Timers_Click(object sender, RoutedEventArgs e) => ToggleTimers();

    // ================= réglages (panneau intégré) =================
    private HkAction? _capturing;
    private Button? _capturingBtn;

    private void Settings_Click(object sender, RoutedEventArgs e)
    {
        if (Settings.Visibility == Visibility.Visible) { CloseSettings(); return; }

        // les raccourcis globaux avalent les touches : on les libère le temps des réglages,
        // sinon appuyer sur F1 déclencherait un minuteur au lieu d'être capturé.
        _hotkeys.Unregister(new WindowInteropHelper(this).Handle);

        SlBg.Value = Math.Round(_timers.BackgroundOpacity * 100);
        SlWin.Value = Math.Round(_timers.Opacity * 100);
        BuildKeyList();
        Settings.Visibility = Visibility.Visible;
        Keyboard.Focus(this);
    }

    private void CloseSettings_Click(object sender, RoutedEventArgs e) => CloseSettings();

    private void CloseSettings()
    {
        _capturing = null; _capturingBtn = null;
        Settings.Visibility = Visibility.Collapsed;
        _hotkeys.Save();
        RegisterHotkeys();
    }

    private void BuildKeyList()
    {
        KeyList.Children.Clear();
        foreach (var (action, label) in HotkeyManager.Actions)
        {
            var row = new DockPanel { Margin = new Thickness(0, 3, 0, 3) };

            var btn = new Button
            {
                Content = _hotkeys.Map.TryGetValue(action, out var hk) ? hk.ToString() : "(aucune)",
                Tag = action,
                Padding = new Thickness(10, 4, 10, 4),
                MinWidth = 160
            };
            btn.Click += Capture_Click;
            DockPanel.SetDock(btn, Dock.Right);
            row.Children.Add(btn);

            row.Children.Add(new TextBlock
            {
                Text = label,
                Foreground = new SolidColorBrush(Color.FromRgb(0x8C, 0x94, 0xA6)),
                FontFamily = new FontFamily("Segoe UI"),
                VerticalAlignment = VerticalAlignment.Center,
                TextWrapping = TextWrapping.Wrap
            });

            KeyList.Children.Add(row);
        }
    }

    private void Capture_Click(object sender, RoutedEventArgs e)
    {
        if (sender is not Button b || b.Tag is not HkAction a) return;
        if (_capturingBtn is not null && _capturingBtn.Tag is HkAction prev)
            _capturingBtn.Content = _hotkeys.Map[prev].ToString();
        _capturing = a;
        _capturingBtn = b;
        b.Content = "appuyez sur une touche…";
        Keyboard.Focus(this);
    }

    protected override void OnPreviewKeyDown(KeyEventArgs e)
    {
        if (_capturing is null) { base.OnPreviewKeyDown(e); return; }
        e.Handled = true;

        var key = e.Key == Key.System ? e.SystemKey : e.Key;
        if (key is Key.LeftCtrl or Key.RightCtrl or Key.LeftAlt or Key.RightAlt
                or Key.LeftShift or Key.RightShift or Key.LWin or Key.RWin) return;

        if (key == Key.Escape)
        {
            if (_capturingBtn is not null && _capturingBtn.Tag is HkAction c)
                _capturingBtn.Content = _hotkeys.Map[c].ToString();
            _capturing = null; _capturingBtn = null;
            return;
        }

        uint mods = 0;
        var m = Keyboard.Modifiers;
        if (m.HasFlag(ModifierKeys.Alt)) mods |= 1;
        if (m.HasFlag(ModifierKeys.Control)) mods |= 2;
        if (m.HasFlag(ModifierKeys.Shift)) mods |= 4;

        AssignCaptured(new Hotkey(mods, (uint)KeyInterop.VirtualKeyFromKey(key)));
    }

    private void AssignCaptured(Hotkey hk)
    {
        if (_capturing is null) return;

        foreach (var (other, _) in HotkeyManager.Actions)
            if (other != _capturing.Value && _hotkeys.Map[other].Mods == hk.Mods && _hotkeys.Map[other].Vk == hk.Vk)
                _hotkeys.Map[other] = new Hotkey(0, 0);

        _hotkeys.Map[_capturing.Value] = hk;
        _capturing = null; _capturingBtn = null;
        _hotkeys.Save();
        BuildKeyList();
    }

    private void Defaults_Click(object sender, RoutedEventArgs e)
    {
        _hotkeys.SetDefaults();
        _hotkeys.Save();
        BuildKeyList();
    }

    private void Bg_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (LblBg is null || !_uiReady) return;
        LblBg.Text = (int)SlBg.Value + " %";
        _timers.BackgroundOpacity = SlBg.Value / 100.0;
        _timers.SaveSettings();
    }

    private void Win_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        if (LblWin is null || !_uiReady) return;
        LblWin.Text = (int)SlWin.Value + " %";
        _timers.Opacity = SlWin.Value / 100.0;
        _timers.SaveSettings();
    }

    private void Bar_Drag(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 2) { BasculerAgrandissement(); return; }       // double-clic = agrandir/restaurer
        if (e.ButtonState != MouseButtonState.Pressed) return;

        // Agrandie, la fenêtre ne peut pas être déplacée : on la restaure d'abord,
        // puis on continue le glissement comme si de rien n'était.
        if (WindowState == WindowState.Maximized)
        {
            WindowState = WindowState.Normal;
            MajBoutonMax();
        }
        try { DragMove(); } catch { }
    }

    private void Max_Click(object sender, RoutedEventArgs e) => BasculerAgrandissement();

    private void BasculerAgrandissement()
    {
        WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        MajBoutonMax();
        SaveSettings();
    }

    private void MajBoutonMax()
    {
        var agrandie = WindowState == WindowState.Maximized;
        BtnMax.Content = agrandie ? "❐" : "▢";
        BtnMax.ToolTip = agrandie ? "Restaurer la taille précédente" : "Agrandir (double-clic sur la barre)";
    }

    private void Min_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;
    private void Close_Click(object sender, RoutedEventArgs e) => Close();

    private void Top_Click(object sender, RoutedEventArgs e)
    {
        Topmost = !Topmost;
        UpdateTopButton();
        SaveSettings();
    }

    private void UpdateTopButton()
    {
        BtnTop.Opacity = Topmost ? 1.0 : 0.45;
        BtnTop.ToolTip = Topmost ? "Toujours au-dessus : activé" : "Toujours au-dessus : désactivé";
    }

    private void Opacity_Changed(object sender, SelectionChangedEventArgs e)
    {
        if (!_uiReady) return;
        if (CbOpacity?.SelectedItem is ComboBoxItem it && it.Tag is string tag &&
            double.TryParse(tag, NumberStyles.Float, CultureInfo.InvariantCulture, out var v))
        {
            Opacity = v;
            SaveSettings();
        }
    }

    // ---------- réglages de fenêtre ----------
    protected override void OnClosed(EventArgs e)
    {
        _hotkeys.Unregister(new WindowInteropHelper(this).Handle);
        _mouse.Dispose();
        base.OnClosed(e);
    }

    private void SaveSettings()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(SettingsPath)!);
            var inv = CultureInfo.InvariantCulture;
            // agrandie, Left/Width décrivent l'écran : on garde la taille d'AVANT l'agrandissement
            var r = WindowState == WindowState.Maximized
                ? RestoreBounds
                : new Rect(Left, Top, Width, Height);

            File.WriteAllText(SettingsPath, string.Join(";",
                r.Left.ToString(inv), r.Top.ToString(inv), r.Width.ToString(inv), r.Height.ToString(inv),
                Opacity.ToString(inv), Topmost ? "1" : "0",
                WindowState == WindowState.Maximized ? "1" : "0",
                "v2"));   // marqueur : le journal n'est plus épinglé par défaut
        }
        catch { }
    }

    private void LoadSettings()
    {
        try
        {
            if (!File.Exists(SettingsPath)) return;
            var p = File.ReadAllText(SettingsPath).Split(';');
            var inv = CultureInfo.InvariantCulture;
            if (p.Length >= 6
                && double.TryParse(p[0], NumberStyles.Float, inv, out var l)
                && double.TryParse(p[1], NumberStyles.Float, inv, out var t)
                && double.TryParse(p[2], NumberStyles.Float, inv, out var w)
                && double.TryParse(p[3], NumberStyles.Float, inv, out var hh)
                && double.TryParse(p[4], NumberStyles.Float, inv, out var o))
            {
                WindowStartupLocation = WindowStartupLocation.Manual;
                Left = l; Top = t;
                Width = Math.Max(MinWidth, w);
                Height = Math.Max(MinHeight, hh);
                Opacity = Math.Clamp(o, 0.3, 1.0);
                Topmost = p[5] == "1";
            }
            if (p.Length >= 7 && p[6] == "1") WindowState = WindowState.Maximized;

            // Fichier d'avant le changement : le journal y était épinglé par défaut, ce qui
            // sortait le jeu du plein écran. On le désépingle une fois ; ensuite le choix
            // de l'utilisateur est respecté.
            if (p.Length < 8 || p[7] != "v2") Topmost = false;
        }
        catch { }
        finally { UpdateTopButton(); SyncOpacityCombo(); MajBoutonMax(); }
    }

    /// <summary>Sélectionne dans la liste la valeur la plus proche de l'opacité en cours.</summary>
    private void SyncOpacityCombo()
    {
        ComboBoxItem? best = null;
        var ecart = double.MaxValue;
        foreach (var o in CbOpacity.Items)
        {
            if (o is not ComboBoxItem it || it.Tag is not string tag) continue;
            if (!double.TryParse(tag, NumberStyles.Float, CultureInfo.InvariantCulture, out var v)) continue;
            var d = Math.Abs(v - Opacity);
            if (d < ecart) { ecart = d; best = it; }
        }
        if (best is not null) CbOpacity.SelectedItem = best;
    }
}
