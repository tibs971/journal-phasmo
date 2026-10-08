using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Threading;

namespace PhasmoCompanion;

/// <summary>
/// Écoute globale des boutons de souris autres que le clic gauche et le clic droit
/// (molette, boutons latéraux 4 et 5). RegisterHotKey ne gère que le clavier :
/// pour la souris il faut un hook bas niveau, comme ici.
/// Les clics gauche et droit ne sont jamais interceptés, pour ne pas gêner le jeu.
/// </summary>
public sealed class MouseHook : IDisposable
{
    public const uint MOUSE_MIDDLE = 0xF001;  // clic molette
    public const uint MOUSE_X1 = 0xF002;      // bouton 4 (précédent)
    public const uint MOUSE_X2 = 0xF003;      // bouton 5 (suivant)

    public static bool IsMouse(uint vk) => vk >= MOUSE_MIDDLE && vk <= MOUSE_X2;

    public static string Label(uint vk) => vk switch
    {
        MOUSE_MIDDLE => "Clic molette",
        MOUSE_X1 => "Souris 4",
        MOUSE_X2 => "Souris 5",
        _ => "?"
    };

    private const int WH_MOUSE_LL = 14;
    private const int WM_MBUTTONDOWN = 0x0207;
    private const int WM_XBUTTONDOWN = 0x020B;

    private delegate IntPtr HookProc(int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr SetWindowsHookEx(int idHook, HookProc lpfn, IntPtr hMod, uint dwThreadId);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool UnhookWindowsHookEx(IntPtr hhk);
    [DllImport("user32.dll")]
    private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);
    [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr GetModuleHandle(string lpModuleName);

    [StructLayout(LayoutKind.Sequential)]
    private struct MSLLHOOKSTRUCT
    {
        public int x, y;
        public uint mouseData;
        public uint flags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    private readonly HookProc _proc;      // gardé en champ : sinon le GC le ramasse
    private IntPtr _hook = IntPtr.Zero;
    private readonly Dispatcher _dispatcher = Dispatcher.CurrentDispatcher;

    /// <summary>Déclenché sur le fil de l'interface avec MOUSE_MIDDLE, MOUSE_X1 ou MOUSE_X2.</summary>
    public event Action<uint>? ButtonPressed;

    public MouseHook()
    {
        _proc = Callback;
        try
        {
            using var module = Process.GetCurrentProcess().MainModule!;
            _hook = SetWindowsHookEx(WH_MOUSE_LL, _proc, GetModuleHandle(module.ModuleName), 0);
        }
        catch { _hook = IntPtr.Zero; }
    }

    private IntPtr Callback(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0)
        {
            var msg = wParam.ToInt32();
            uint vk = 0;

            if (msg == WM_MBUTTONDOWN) vk = MOUSE_MIDDLE;
            else if (msg == WM_XBUTTONDOWN)
            {
                var data = Marshal.PtrToStructure<MSLLHOOKSTRUCT>(lParam);
                vk = (data.mouseData >> 16) == 1 ? MOUSE_X1 : MOUSE_X2;
            }

            if (vk != 0)
            {
                var handler = ButtonPressed;
                if (handler is not null) _dispatcher.BeginInvoke(handler, vk);
            }
        }
        return CallNextHookEx(_hook, nCode, wParam, lParam);
        // on laisse toujours passer le clic : l'appli réagit sans voler l'événement au jeu
    }

    public void Dispose()
    {
        if (_hook != IntPtr.Zero) UnhookWindowsHookEx(_hook);
        _hook = IntPtr.Zero;
    }
}
