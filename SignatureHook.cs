using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Threading;

namespace PhasmoCompanion;

/// <summary>
/// Écoute globale de la combinaison qui ouvre la signature d'auteur :
/// <b>Ctrl + Alt gauche + )</b> — la touche « ) » est celle juste à droite du 0
/// sur un clavier français (code OEM_4, dépendant de la disposition clavier).
///
/// L'Alt droit (AltGr) est explicitement refusé. Sous Windows, AltGr équivaut à
/// Ctrl + Alt : sans ce refus, un raccourci « Ctrl + Alt » se déclencherait aussi
/// en tapant un caractère obtenu avec AltGr. Un raccourci enregistré par
/// RegisterHotKey ne sait pas distinguer l'Alt gauche de l'Alt droit, d'où ce hook
/// bas niveau, qui lit l'état de chaque touche séparément.
///
/// L'Alt gauche étant obligatoire, taper « ) » normalement ne déclenche rien, et
/// « ] » (AltGr + parenthèse) non plus puisque AltGr est refusé.
/// Le hook ne consomme jamais aucune touche — le jeu reçoit tout normalement.
/// </summary>
public sealed class SignatureHook : IDisposable
{
    private const int WH_KEYBOARD_LL = 13;
    private const int WM_KEYDOWN = 0x0100;
    private const int WM_SYSKEYDOWN = 0x0104;   // une touche pressée avec Alt arrive par ce message

    private const uint VK_LCONTROL = 0xA2;
    private const uint VK_RCONTROL = 0xA3;
    private const uint VK_LMENU = 0xA4;         // Alt gauche
    private const uint VK_RMENU = 0xA5;         // Alt droit = AltGr
    private const uint VK_PARENTHESE = 0xDB;    // touche « ) ° ] » du clavier français

    private delegate IntPtr HookProc(int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr SetWindowsHookEx(int idHook, HookProc lpfn, IntPtr hMod, uint dwThreadId);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool UnhookWindowsHookEx(IntPtr hhk);
    [DllImport("user32.dll")]
    private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);
    [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr GetModuleHandle(string lpModuleName);
    [DllImport("user32.dll")]
    private static extern short GetAsyncKeyState(int vKey);

    [StructLayout(LayoutKind.Sequential)]
    private struct KBDLLHOOKSTRUCT
    {
        public uint vkCode;
        public uint scanCode;
        public uint flags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    private readonly HookProc _proc;          // gardé en champ : sinon le GC le ramasse
    private IntPtr _hook = IntPtr.Zero;
    private readonly Dispatcher _dispatcher = Dispatcher.CurrentDispatcher;

    /// <summary>Déclenché sur le fil de l'interface quand la combinaison est reconnue.</summary>
    public event Action? Declenche;

    public SignatureHook()
    {
        _proc = Callback;
        try
        {
            using var module = Process.GetCurrentProcess().MainModule!;
            _hook = SetWindowsHookEx(WH_KEYBOARD_LL, _proc, GetModuleHandle(module.ModuleName), 0);
        }
        catch { _hook = IntPtr.Zero; }
    }

    private static bool Enfoncee(uint vk) => (GetAsyncKeyState((int)vk) & 0x8000) != 0;

    private IntPtr Callback(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0)
        {
            var msg = wParam.ToInt32();
            if (msg is WM_KEYDOWN or WM_SYSKEYDOWN)
            {
                var k = Marshal.PtrToStructure<KBDLLHOOKSTRUCT>(lParam);
                if (k.vkCode == VK_PARENTHESE && CombinaisonValide())
                {
                    var h = Declenche;
                    if (h is not null) _dispatcher.BeginInvoke(h);
                }
            }
        }
        return CallNextHookEx(_hook, nCode, wParam, lParam);
        // jamais de touche avalée : le jeu et la saisie de texte continuent normalement
    }

    private bool CombinaisonValide()
    {
        if (Enfoncee(VK_RMENU)) return false;                       // AltGr : refusé
        if (!Enfoncee(VK_LMENU)) return false;                      // Alt gauche obligatoire
        return Enfoncee(VK_LCONTROL) || Enfoncee(VK_RCONTROL);      // et un Ctrl
    }

    public void Dispose()
    {
        if (_hook != IntPtr.Zero) UnhookWindowsHookEx(_hook);
        _hook = IntPtr.Zero;
    }
}
