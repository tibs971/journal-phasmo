using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows.Input;

namespace PhasmoCompanion;

public enum HkAction
{
    Smudge, Hunt, Cooldown, ResetAll,
    ResetSmudge, ResetHunt, ResetCooldown,
    Tap, TapReset, ToggleTimers, TogglePin
}

/// <summary>Une touche + ses modificateurs.</summary>
public sealed class Hotkey
{
    public uint Mods;   // MOD_ALT 1, MOD_CONTROL 2, MOD_SHIFT 4
    public uint Vk;     // code de touche virtuelle Windows

    public Hotkey(uint mods, uint vk) { Mods = mods; Vk = vk; }

    public bool IsSet => Vk != 0;

    public bool IsMouse => MouseHook.IsMouse(Vk);

    public override string ToString()
    {
        if (!IsSet) return "(aucune)";
        var s = "";
        if ((Mods & 2) != 0) s += "Ctrl + ";
        if ((Mods & 1) != 0) s += "Alt + ";
        if ((Mods & 4) != 0) s += "Maj + ";
        if (IsMouse) return s + MouseHook.Label(Vk);

        var key = KeyInterop.KeyFromVirtualKey((int)Vk);
        var name = key switch
        {
            >= Key.NumPad0 and <= Key.NumPad9 => "Pavé " + (key - Key.NumPad0),
            Key.Add => "Pavé +",
            Key.Subtract => "Pavé -",
            Key.Multiply => "Pavé *",
            Key.Divide => "Pavé /",
            Key.Decimal => "Pavé .",
            _ => key.ToString()
        };
        return s + name;
    }

    public string Serialize() => Mods.ToString(CultureInfo.InvariantCulture) + "," + Vk.ToString(CultureInfo.InvariantCulture);

    public static Hotkey Parse(string s)
    {
        var p = s.Split(',');
        return p.Length == 2
               && uint.TryParse(p[0], out var m)
               && uint.TryParse(p[1], out var v)
            ? new Hotkey(m, v)
            : new Hotkey(0, 0);
    }
}

/// <summary>
/// Enregistre les raccourcis auprès de Windows (ils fonctionnent même quand le jeu a le focus)
/// et les relit depuis %AppData% pour qu'ils soient modifiables sans recompiler.
/// </summary>
public sealed class HotkeyManager
{
    [DllImport("user32.dll")] private static extern bool RegisterHotKey(IntPtr hWnd, int id, uint mods, uint vk);
    [DllImport("user32.dll")] private static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    private const uint MOD_NOREPEAT = 0x4000;

    public static readonly (HkAction Action, string Label)[] Actions =
    {
        (HkAction.Smudge,        "Encens : démarrer / pause"),
        (HkAction.Hunt,          "Chasse : démarrer / pause"),
        (HkAction.Cooldown,      "Recharge : démarrer / pause"),
        (HkAction.ResetAll,      "Remettre les 3 minuteurs à zéro"),
        (HkAction.ResetSmudge,   "Remettre l'encens à zéro"),
        (HkAction.ResetHunt,     "Remettre la chasse à zéro"),
        (HkAction.ResetCooldown, "Remettre la recharge à zéro"),
        (HkAction.Tap,           "Taper le rythme des pas"),
        (HkAction.TapReset,      "Remettre la mesure de vitesse à zéro"),
        (HkAction.ToggleTimers,  "Afficher / masquer les minuteurs"),
        (HkAction.TogglePin,     "Épingler la fenêtre du journal"),
    };

    public readonly Dictionary<HkAction, Hotkey> Map = new();

    /// <summary>
    /// Raccourci discret de la signature d'auteur : Ctrl + Alt + la touche « @ »
    /// (celle du 0 sur un clavier français). Volontairement absent de la liste Actions :
    /// il n'apparaît donc ni dans le panneau des réglages, ni dans le fichier enregistré.
    /// </summary>
    public const int IdSignature = 999;
    public const int IdSignatureBis = 998;
    public readonly Hotkey Signature = new(2 | 1, 0x30);       // Ctrl + Alt + 0/@
    public readonly Hotkey SignatureBis = new(2 | 1, 0x60);    // secours : Ctrl + Alt + pavé 0

    private static readonly string Path_ = System.IO.Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion", "hotkeys.txt");

    public HotkeyManager()
    {
        SetDefaults();
        Load();
    }

    public void SetDefaults()
    {
        Map[HkAction.Smudge] = new Hotkey(0, 0x70);          // F1
        Map[HkAction.Hunt] = new Hotkey(0, 0x71);            // F2
        Map[HkAction.Cooldown] = new Hotkey(0, 0x72);        // F3
        Map[HkAction.ResetAll] = new Hotkey(0, 0x73);        // F4
        Map[HkAction.Tap] = new Hotkey(0, 0x74);             // F5
        Map[HkAction.ToggleTimers] = new Hotkey(0, 0x75);    // F6
        Map[HkAction.TogglePin] = new Hotkey(0, 0x76);       // F7
        Map[HkAction.ResetSmudge] = new Hotkey(4, 0x70);     // Maj+F1
        Map[HkAction.ResetHunt] = new Hotkey(4, 0x71);       // Maj+F2
        Map[HkAction.ResetCooldown] = new Hotkey(4, 0x72);   // Maj+F3
        Map[HkAction.TapReset] = new Hotkey(4, 0x74);        // Maj+F5
    }

    public void Load()
    {
        try
        {
            if (!File.Exists(Path_)) return;
            foreach (var line in File.ReadAllLines(Path_))
            {
                var kv = line.Split('=');
                if (kv.Length == 2 && Enum.TryParse<HkAction>(kv[0], out var a))
                    Map[a] = Hotkey.Parse(kv[1]);
            }
        }
        catch { }
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(System.IO.Path.GetDirectoryName(Path_)!);
            var lines = new List<string>();
            foreach (var (a, _) in Actions)
                lines.Add(a + "=" + Map[a].Serialize());
            File.WriteAllLines(Path_, lines);
        }
        catch { }
    }

    /// <summary>Enregistre tout. Renvoie la liste des actions dont la touche a été refusée (déjà prise).</summary>
    public List<string> Register(IntPtr hwnd)
    {
        var failed = new List<string>();
        foreach (var (a, label) in Actions)
        {
            var hk = Map[a];
            if (!hk.IsSet || hk.IsMouse) continue;   // la souris passe par MouseHook
            if (!RegisterHotKey(hwnd, (int)a + 100, hk.Mods | MOD_NOREPEAT, hk.Vk))
                failed.Add($"{label} ({hk})");
        }
        RegisterHotKey(hwnd, IdSignature, Signature.Mods | MOD_NOREPEAT, Signature.Vk);
        RegisterHotKey(hwnd, IdSignatureBis, SignatureBis.Mods | MOD_NOREPEAT, SignatureBis.Vk);
        return failed;
    }

    public void Unregister(IntPtr hwnd)
    {
        foreach (var (a, _) in Actions) UnregisterHotKey(hwnd, (int)a + 100);
        UnregisterHotKey(hwnd, IdSignature);
        UnregisterHotKey(hwnd, IdSignatureBis);
    }

    public static HkAction FromId(int id) => (HkAction)(id - 100);
}
