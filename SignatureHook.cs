using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Threading;

namespace PhasmoCompanion;

/// <summary>
/// Écoute globale de la combinaison qui ouvre la signature d'auteur : Ctrl + Alt + }
/// (la touche « } » est celle du « = » sur un clavier français).
///
/// Pourquoi un hook bas niveau plutôt que RegisterHotKey : sous Windows, AltGr EST
/// Ctrl + Alt. Un raccourci enregistré en Ctrl + Alt + } se déclencherait donc à chaque
/// fois qu'on tape simplement « } », ce qui est insupportable. Il faut distinguer le
/// Ctrl réellement enfoncé du Ctrl fantôme que Windows fabrique quand on presse AltGr.
///
/// La ruse : quand on appuie sur AltGr, Windows envoie un Ctrl gauche et un Alt droit
/// portant le MÊME horodatage. Un Ctrl pressé à la main, lui, arrive à un autre moment.
/// On ne retient donc que le premier appui de Ctrl gauche (les répétitions et le doublon
/// d'AltGr sont ignorés) et on compare son horodatage à celui d'AltGr.
///
/// Le hook ne consomme jamais aucune touche : le jeu reçoit tout normalement.
/// </summary>
public sealed class SignatureHook : IDisposable
{
    private const int WH_KEYBOARD_LL = 13;
    private const int WM_KEYDOWN = 0x0100;
    private const int WM_SYSKEYDOWN = 0x0104;
    private const int WM_KEYUP = 0x0101;
    private const int WM_SYSKEYUP = 0x0105;

    private const uint VK_LCONTROL = 0xA2;
    private const uint VK_RCONTROL = 0xA3;
    private const uint VK_LMENU = 0xA4;      // Alt gauche
    private const uint VK_RMENU = 0xA5;      // Alt droit = AltGr
    private const uint VK_OEM_PLUS = 0xBB;   // touche « = + } » sur clavier français

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

    private bool _ctrlGaucheEnfonce;
    private uint _tCtrlGauche;                // horodatage du PREMIER appui de Ctrl gauche
    private uint _tAltGr;                     // horodatage du dernier appui d'AltGr

    /// <summary>Déclenché sur le fil de l'interface quand la combinaison complète est reconnue.</summary>
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
            var k = Marshal.PtrToStructure<KBDLLHOOKSTRUCT>(lParam);

            if (msg is WM_KEYDOWN or WM_SYSKEYDOWN)
            {
                switch (k.vkCode)
                {
                    case VK_LCONTROL:
                        // seulement le premier appui : on ignore la répétition automatique
                        // et le Ctrl fantôme ajouté par AltGr quand Ctrl est déjà tenu
                        if (!_ctrlGaucheEnfonce) { _ctrlGaucheEnfonce = true; _tCtrlGauche = k.time; }
                        break;

                    case VK_RMENU:
                        _tAltGr = k.time;
                        break;

                    case VK_OEM_PLUS:
                        if (CombinaisonValide())
                        {
                            var h = Declenche;
                            if (h is not null) _dispatcher.BeginInvoke(h);
                        }
                        break;
                }
            }
            else if (msg is WM_KEYUP or WM_SYSKEYUP && k.vkCode == VK_LCONTROL)
            {
                _ctrlGaucheEnfonce = false;
            }
        }
        return CallNextHookEx(_hook, nCode, wParam, lParam);
        // jamais de touche avalée : le jeu et la saisie de texte continuent normalement
    }

    private bool CombinaisonValide()
    {
        var altGr = Enfoncee(VK_RMENU);
        var altGauche = Enfoncee(VK_LMENU);
        if (!altGr && !altGauche) return false;          // il faut un Alt

        // Ctrl droit : toujours une vraie frappe, Windows ne le fabrique jamais.
        if (Enfoncee(VK_RCONTROL)) return true;

        if (!Enfoncee(VK_LCONTROL)) return false;

        // Ctrl gauche : vrai seulement s'il n'est pas celui qu'AltGr vient de fabriquer.
        // Fabriqué = même horodatage que l'appui d'AltGr.
        return !altGr || _tCtrlGauche != _tAltGr;
    }

    public void Dispose()
    {
        if (_hook != IntPtr.Zero) UnhookWindowsHookEx(_hook);
        _hook = IntPtr.Zero;
    }
}
