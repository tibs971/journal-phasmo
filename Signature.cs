using System;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;

namespace PhasmoCompanion;

/// <summary>
/// Panneau de signature de l'auteur, ouvert par un raccourci discret (Ctrl + Alt + @).
///
/// Les deux codes ne sont pas écrits en clair : seules leurs empreintes SHA-256 salées
/// figurent ici, donc un simple « strings PhasmoCompanion.exe » ne les révèle pas.
/// À dire franchement quand même : quelqu'un qui décompile l'exe peut retirer ce contrôle.
/// C'est une signature d'auteur, pas une serrure. La preuve d'antériorité solide, c'est
/// l'empreinte de l'exe publiée quelque part de daté (voir Empreinte ci-dessous).
/// </summary>
public static class Signature
{
    private const string Sel = "JournalPhasmo::Shwarzyi::";

    // Deux empreintes par étape : le code avec et sans sa virgule finale, pour ne pas
    // se retrouver bloqué dehors sur une virgule de ponctuation.
    private static readonly string[] Code1 =
    {
        "b845db76051fdad6f5ec13fcf7f47d110ec700a03dd5a2400336b55df15a1af5",
        "b668fdf77f88660b3a36741f3faebbbdf985596553d6d93757a2491edaced676"
    };
    private static readonly string[] Code2 =
    {
        "ade6c5398ab7c18954dee52920f06ffc53aded04c63e3bde4154cc8e0d6e2e83",
        "96a0c057b936dc73cbf36284d6fb200134357424d354418989f9ee4f6e99077b"
    };

    public const string Auteur = "Thibault.P (Shwarzyi)";
    public const string DateCreation = "7 octobre 2026";
    public const string VersionAppli = "1.0";

    private static string Empreinte(string texte)
    {
        var h = SHA256.HashData(Encoding.UTF8.GetBytes(Sel + texte));
        var sb = new StringBuilder(h.Length * 2);
        foreach (var b in h) sb.Append(b.ToString("x2", CultureInfo.InvariantCulture));
        return sb.ToString();
    }

    /// <summary>Comparaison à durée constante : pas d'indice sur le nombre de caractères justes.</summary>
    private static bool Egal(string a, string b)
    {
        if (a.Length != b.Length) return false;
        var diff = 0;
        for (var i = 0; i < a.Length; i++) diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static bool Correspond(string saisie, string[] attendues)
    {
        var e = Empreinte((saisie ?? "").Trim());
        var ok = false;
        foreach (var a in attendues) ok |= Egal(e, a);   // pas de sortie anticipée : durée stable
        return ok;
    }

    public static bool PremierCode(string saisie) => Correspond(saisie, Code1);
    public static bool SecondCode(string saisie) => Correspond(saisie, Code2);

    /// <summary>
    /// Empreinte SHA-256 du fichier exécutable en cours. Deux exe identiques donnent la même
    /// empreinte, un exe modifié en donne une autre : c'est ce nombre qu'il faut publier
    /// quelque part d'horodaté pour pouvoir prouver, plus tard, ce qui existait et quand.
    /// </summary>
    public static string EmpreinteExe()
    {
        try
        {
            var chemin = Environment.ProcessPath;
            if (string.IsNullOrEmpty(chemin) || !File.Exists(chemin)) return "indisponible";
            using var flux = File.OpenRead(chemin);
            var h = SHA256.HashData(flux);
            var sb = new StringBuilder(h.Length * 2);
            foreach (var b in h) sb.Append(b.ToString("x2", CultureInfo.InvariantCulture));
            var hex = sb.ToString().ToUpperInvariant();
            // groupé par 8 pour être lisible et recopiable à la main
            var jolie = new StringBuilder();
            for (var i = 0; i < hex.Length; i += 8)
            {
                if (i > 0) jolie.Append(i % 32 == 0 ? '\n' : ' ');
                jolie.Append(hex, i, Math.Min(8, hex.Length - i));
            }
            return jolie.ToString();
        }
        catch { return "indisponible"; }
    }

    /// <summary>Date de compilation de cet exe, lue dans l'en-tête PE.</summary>
    public static string DateCompilation()
    {
        try
        {
            var chemin = Environment.ProcessPath;
            if (!string.IsNullOrEmpty(chemin) && File.Exists(chemin))
                return File.GetLastWriteTime(chemin).ToString("d MMMM yyyy 'à' HH:mm", new CultureInfo("fr-FR"));
        }
        catch { }
        return "inconnue";
    }

    public static string VersionDotNet => Environment.Version.ToString();

    public static string VersionWebView2
    {
        get
        {
            try
            {
                var asm = AppDomain.CurrentDomain.GetAssemblies();
                foreach (var a in asm)
                {
                    var n = a.GetName();
                    if (n.Name == "Microsoft.Web.WebView2.Core") return n.Version?.ToString() ?? "?";
                }
                return "1.0.2792.45";
            }
            catch { return "1.0.2792.45"; }
        }
    }

    public static string VersionAssemblage =>
        Assembly.GetExecutingAssembly().GetName().Version?.ToString() ?? VersionAppli;
}
