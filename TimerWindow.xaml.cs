using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Threading;

namespace PhasmoCompanion;

/// <summary>
/// Fenêtre flottante des minuteurs, façon LiveSplit : sans bordure, toujours au-dessus,
/// déplaçable, pilotée par les raccourcis globaux enregistrés dans MainWindow.
/// </summary>
public partial class TimerWindow : Window
{
    public sealed class Chrono
    {
        private readonly Stopwatch _sw = new();
        public bool Running => _sw.IsRunning;
        public bool Paused { get; private set; }
        public double Seconds => _sw.Elapsed.TotalSeconds;
        public void Toggle() { if (_sw.IsRunning) { _sw.Stop(); Paused = true; } else { _sw.Start(); Paused = false; } }
        public void Restart() { _sw.Restart(); Paused = false; }
        public void Reset() { _sw.Reset(); Paused = false; }
    }

    public readonly Chrono Smudge = new();
    public readonly Chrono Hunt = new();
    public readonly Chrono Cd = new();

    private readonly DispatcherTimer _tick = new() { Interval = TimeSpan.FromMilliseconds(100) };

    private static readonly string SettingsPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion", "timers.txt");

    public TimerWindow()
    {
        InitializeComponent();

        foreach (var b in new[] { BoxSmudge, BoxHunt, BoxCd })
        {
            b.Background = _boxBg;
            b.BorderBrush = _boxLine;
        }
        Grip.Background = GripBrush;

        LoadSettings();
        _tick.Tick += (_, _) => Refresh();
        _tick.Start();
        Refresh();
    }

    // touches affichées : remplacées par celles réellement configurées
    private string _kSmudge = "F1", _kHunt = "F2", _kCd = "F3", _kReset = "F4";

    // difficulté courante, envoyée par le journal
    private string _diff = "Professionnel";
    private int _grace = 3, _hSmall = 30, _hMed = 50, _hLarge = 60;
    /// <summary>La fenêtre était-elle affichée la dernière fois qu'on a quitté ?</summary>
    public bool EtaitVisible { get; private set; } = true;

    private int _size = -1;                 // -1 inconnue, 0 petite, 1 moyenne, 2 grande
    private static readonly string[] SizeName = { "petite carte", "carte moyenne", "grande carte" };

    /// <summary>Affiche les touches réellement configurées à côté de chaque minuteur.</summary>
    public void ShowKeys(string smudge, string hunt, string cd, string resetAll, string hide)
    {
        _kSmudge = smudge; _kHunt = hunt; _kCd = cd; _kReset = resetAll;
        MiHide.Header = $"Masquer ({hide})";
        LblSmudge.Text = "Encens   " + smudge;
        LblHunt.Text = "Chasse   " + hunt;
        LblCd.Text = "Recharge   " + cd;
        MiResetAll.Header = $"Tout remettre à zéro ({resetAll})";
        WriteHint();
        Refresh();
    }

    /// <summary>Difficulté choisie dans le journal : change la grâce et les durées de chasse affichées.</summary>
    public void SetDifficulty(string label, int grace, int small, int med, int large, int size)
    {
        _diff = string.IsNullOrWhiteSpace(label) ? "—" : label;
        _grace = Math.Clamp(grace, 0, 10);
        _hSmall = Math.Max(1, small); _hMed = Math.Max(_hSmall, med); _hLarge = Math.Max(_hMed, large);
        _size = size is >= 0 and <= 2 ? size : -1;
        WriteHint();
        Refresh();
    }

    /// <summary>Durée de la chasse sur la carte choisie, ou 0 si la taille est inconnue.</summary>
    private int HuntLen => _size switch { 0 => _hSmall, 1 => _hMed, 2 => _hLarge, _ => 0 };

    private void WriteHint()
    {
        var carte = _size >= 0 ? " · " + SizeName[_size] : "";
        HintLine.Text = $"{_kSmudge}/{_kHunt}/{_kCd} démarrer ou pause · {_kReset} remise à zéro · {_diff}{carte}";
    }

    // ---------- appelé par les raccourcis globaux ----------
    public void ToggleSmudge() { Smudge.Toggle(); Refresh(); }
    public void ToggleCd() { Cd.Toggle(); Refresh(); }

    public void ToggleHunt()
    {
        bool wasRunning = Hunt.Running;
        Hunt.Toggle();
        if (wasRunning && MiAutoCd.IsChecked) Cd.Restart();   // la chasse se termine → recharge
        Refresh();
    }

    public void ResetSmudge() { Smudge.Reset(); Refresh(); }
    public void ResetHunt() { Hunt.Reset(); Refresh(); }
    public void ResetCd() { Cd.Reset(); Refresh(); }
    public void ResetAll() { Smudge.Reset(); Hunt.Reset(); Cd.Reset(); Refresh(); }

    // ---------- affichage ----------
    private static string Fmt(double s) => ((int)(s / 60)) + ":" + ((int)(s % 60)).ToString("00");

    // couleurs du TEXTE : toujours pleinement opaques
    private static readonly Brush Ink = new SolidColorBrush(Color.FromRgb(0xE3, 0xE6, 0xEC));
    private static readonly Brush InkBright = new SolidColorBrush(Colors.White);
    private static readonly Brush LblBright = new SolidColorBrush(Color.FromRgb(0xC9, 0xD1, 0xDC));
    private static readonly Brush Muted = new SolidColorBrush(Color.FromRgb(0x8C, 0x94, 0xA6));
    private static readonly Brush Warn = new SolidColorBrush(Color.FromRgb(0xF2, 0xB8, 0x4B));
    private static readonly Brush Good = new SolidColorBrush(Color.FromRgb(0x5C, 0xC9, 0x9A));

    // couleurs du DÉCOR : fonds et contours, pilotés par BackgroundOpacity
    private readonly SolidColorBrush _boxBg = new(Color.FromRgb(0x1E, 0x23, 0x2D));
    private readonly SolidColorBrush _boxLine = new(Color.FromRgb(0x2A, 0x30, 0x3C));
    private readonly SolidColorBrush _boxAccent = new(Color.FromRgb(0xA6, 0x8C, 0xFF));
    private readonly SolidColorBrush _boxWarn = new(Color.FromRgb(0xF2, 0xB8, 0x4B));
    private readonly SolidColorBrush GripBrush = new(Color.FromRgb(0x2A, 0x30, 0x3C));

    private void Refresh()
    {
        Paint(Smudge, TSmudge, MSmudge, BoxSmudge, SmudgeMsg(Smudge.Seconds), SmudgeColor(Smudge.Seconds));
        Paint(Hunt, THunt, MHunt, BoxHunt, HuntMsg(Hunt.Seconds), Muted);
        Paint(Cd, TCd, MCd, BoxCd, CdMsg(Cd.Seconds), CdColor(Cd.Seconds));
    }

    private void Paint(Chrono c, TextBlock time, TextBlock msg, Border box, string text, Brush brush)
    {
        time.Text = Fmt(c.Seconds);
        msg.Text = (c.Paused && c.Seconds > 0 ? "⏸ en pause · " : "") + text;
        msg.Foreground = brush;
        box.BorderBrush = c.Running ? _boxAccent : (c.Paused && c.Seconds > 0 ? _boxWarn : _boxLine);
    }

    // Encens : 60 s Démon · 90 s standard · 180 s Esprit
    private string SmudgeMsg(double s) => s <= 0 ? _kSmudge + " pour démarrer"
        : s < 60 ? "aucune chasse possible"
        : s < 90 ? "chasse possible : Démon"
        : s < 180 ? "chasse possible : sauf Esprit"
        : "toutes peuvent chasser";

    private static Brush SmudgeColor(double s) => s <= 0 ? Muted : s < 60 ? Good : s < 180 ? Warn : Ink;

    private string HuntMsg(double s)
    {
        if (s <= 0) return _kHunt + " pour démarrer";
        if (s < _grace) return $"grâce {_grace} s : pas de mort";

        var len = HuntLen;
        if (len > 0)                       // taille de carte connue : une seule durée
            return s < len ? $"chasse en cours · {len} s" : $"dépassé {len} s : maudite ou morts";

        return s < _hSmall ? "chasse en cours"
             : s < _hMed ? $"finie sur petite carte ({_hSmall} s)"
             : s < _hLarge ? $"finie sauf grande carte ({_hMed} s)"
             : $"au-delà du max ({_hLarge} s)";
    }

    // Recharge : 20 s Démon · 25 s les autres
    private string CdMsg(double s) => s <= 0 ? _kCd + " pour démarrer"
        : s < 20 ? "pas de nouvelle chasse"
        : s < 25 ? "le Démon peut rechasser"
        : "nouvelle chasse possible";

    private static Brush CdColor(double s) => s <= 0 ? Muted : s < 20 ? Good : s < 25 ? Warn : Ink;

    // ---------- fenêtre ----------
    private void Drag(object sender, MouseButtonEventArgs e)
    {
        if (MiLock.IsChecked) return;
        if (e.ButtonState == MouseButtonState.Pressed) DragMove();
    }

    private void ResetAll_Click(object sender, RoutedEventArgs e) => ResetAll();

    /// <summary>
    /// Épinglage des minuteurs. Décoché, la fenêtre cesse de passer devant le jeu — c'est ce
    /// qu'il faut faire si Phasmophobia est en plein écran EXCLUSIF, que Windows interrompt
    /// dès qu'une fenêtre « toujours au-dessus » s'affiche par-dessus lui.
    /// </summary>
    private void Top_Click(object sender, RoutedEventArgs e)
    {
        Topmost = MiTop.IsChecked;
        SaveSettings();
    }
    private void Hide_Click(object sender, RoutedEventArgs e) => Hide();

    /// <summary>
    /// Opacité de tout le décor — cadre, fonds, contours — SANS toucher au texte
    /// ni aux chiffres des chronomètres, qui restent nets même à 0 %.
    /// </summary>
    private double _bgOpacity = 0.85;

    public double BackgroundOpacity
    {
        get => _bgOpacity;
        set
        {
            var v = Math.Clamp(value, 0, 1);
            _bgOpacity = v;

            // 2 % minimum sur le fond : invisible à l'œil, mais la souris peut encore
            // attraper la fenêtre pour la déplacer.
            RootBrush.Opacity = Math.Max(v, 0.02);
            RootBorderBrush.Opacity = v;
            _boxBg.Opacity = Math.Max(v, 0.02);
            _boxLine.Opacity = v;
            _boxAccent.Opacity = v;
            _boxWarn.Opacity = v;

            // la poignée reste toujours saisissable
            GripBrush.Opacity = Math.Max(v, 0.45);

            // sans décor, on renforce le texte pour qu'il reste bien lisible sur le jeu
            var vif = v < 0.35;
            foreach (var tb in new[] { TSmudge, THunt, TCd })
            {
                tb.Foreground = vif ? InkBright : Ink;
                tb.FontWeight = vif ? FontWeights.Bold : FontWeights.SemiBold;
            }
            foreach (var tb in new[] { LblSmudge, LblHunt, LblCd, HintLine })
                tb.Foreground = vif ? LblBright : Muted;

            Refresh();
        }
    }

    private void BgOpacity_Click(object sender, RoutedEventArgs e)
    {
        if (sender is MenuItem m && m.Tag is string tag &&
            double.TryParse(tag, NumberStyles.Float, CultureInfo.InvariantCulture, out var v))
        {
            BackgroundOpacity = v;
            SaveSettings();
        }
    }

    private void Opacity_Click(object sender, RoutedEventArgs e)
    {
        if (sender is MenuItem m && m.Tag is string tag &&
            double.TryParse(tag, NumberStyles.Float, CultureInfo.InvariantCulture, out var v))
        {
            Opacity = v;
            SaveSettings();
        }
    }

    protected override void OnLocationChanged(EventArgs e) { base.OnLocationChanged(e); SaveSettings(); }

    /// <summary>
    /// Position, opacités et options, dans un fichier lisible : une ligne « clé=valeur ».
    /// Une clé absente garde simplement sa valeur par défaut, ce qui évite de tout perdre
    /// quand le fichier vient d'une version plus ancienne.
    /// </summary>
    public void SaveSettings()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(SettingsPath)!);
            var inv = CultureInfo.InvariantCulture;
            File.WriteAllLines(SettingsPath, new[]
            {
                "gauche="  + Left.ToString(inv),
                "haut="    + Top.ToString(inv),
                "fenetre=" + Opacity.ToString(inv),
                "decor="   + BackgroundOpacity.ToString(inv),
                "verrou="  + (MiLock.IsChecked ? "1" : "0"),
                "autocd="  + (MiAutoCd.IsChecked ? "1" : "0"),
                "dessus="  + (MiTop.IsChecked ? "1" : "0"),
                "affichee=" + (IsVisible ? "1" : "0")
            });
        }
        catch { }
    }

    private void LoadSettings()
    {
        var inv = CultureInfo.InvariantCulture;
        var v = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        try
        {
            if (File.Exists(SettingsPath))
            {
                var txt = File.ReadAllText(SettingsPath);

                if (txt.Contains('=') == false && txt.Contains(';'))
                {
                    // ancien format « gauche;haut;fenêtre;verrou;autocd;décor »
                    var p = txt.Split(';');
                    string[] noms = { "gauche", "haut", "fenetre", "verrou", "autocd", "decor", "affichee" };
                    for (var i = 0; i < p.Length && i < noms.Length; i++) v[noms[i]] = p[i];
                }
                else
                {
                    foreach (var ligne in txt.Split('\n'))
                    {
                        var i = ligne.IndexOf('=');
                        if (i > 0) v[ligne[..i].Trim()] = ligne[(i + 1)..].Trim();
                    }
                }
            }
        }
        catch { }

        double? Nb(string k) => v.TryGetValue(k, out var t) &&
            double.TryParse(t, NumberStyles.Float, inv, out var d) && !double.IsNaN(d) ? (double?)d : null;

        var g = Nb("gauche"); var h = Nb("haut");
        if (g is not null && h is not null) { Left = g.Value; Top = h.Value; }

        Opacity = Math.Clamp(Nb("fenetre") ?? 1.0, 0.3, 1.0);
        if (v.TryGetValue("affichee", out var af)) EtaitVisible = af != "0";
        if (v.TryGetValue("verrou", out var lo)) MiLock.IsChecked = lo == "1";
        if (v.TryGetValue("autocd", out var ac)) MiAutoCd.IsChecked = ac == "1";
        if (v.TryGetValue("dessus", out var de)) { MiTop.IsChecked = de == "1"; Topmost = MiTop.IsChecked; }

        // toujours passer par la propriété, même sans fichier : c'est elle qui applique
        // l'opacité à tous les pinceaux du décor et qui renforce le texte si besoin.
        BackgroundOpacity = Math.Clamp(Nb("decor") ?? _bgOpacity, 0, 1);
    }
}
