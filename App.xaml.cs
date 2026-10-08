using System;
using System.IO;
using System.Windows;
using System.Windows.Threading;

namespace PhasmoCompanion;

public partial class App : Application
{
    private static readonly string LogPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion", "erreurs.txt");

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // Une erreur ne doit jamais fermer l'application sans rien dire.
        DispatcherUnhandledException += (_, args) =>
        {
            Report(args.Exception);
            args.Handled = true;
        };

        AppDomain.CurrentDomain.UnhandledException += (_, args) =>
        {
            if (args.ExceptionObject is Exception ex) Report(ex);
        };
    }

    private static void Report(Exception ex)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(LogPath)!);
            File.AppendAllText(LogPath,
                $"{DateTime.Now:yyyy-MM-dd HH:mm:ss}{Environment.NewLine}{ex}{Environment.NewLine}{Environment.NewLine}");
        }
        catch { }

        MessageBox.Show(
            "Une erreur est survenue, mais l'application continue.\n\n" + ex.Message +
            "\n\nDétail complet enregistré dans :\n" + LogPath,
            "Journal Phasmo", MessageBoxButton.OK, MessageBoxImage.Warning);
    }
}
