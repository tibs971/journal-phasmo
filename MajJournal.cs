using System;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;

namespace PhasmoCompanion;

/// <summary>
/// Met à jour le CONTENU du journal sans réinstaller l'application.
///
/// Pourquoi : les fiches d'entités changent à chaque correctif du jeu, la coquille
/// Windows presque jamais. Télécharger tout l'exécutable pour corriger une vitesse
/// serait absurde — le journal seul pèse environ 260 Ko.
///
/// Ce que ça fait exactement : une requête GET vers l'adresse publique du projet, au
/// démarrage et pas plus d'une fois par jour. Rien n'est envoyé : ni identifiant, ni
/// statistique, ni contenu de votre enquête. Le fichier reçu n'est retenu que s'il
/// ressemble vraiment au journal, et la version embarquée reste le filet de sécurité.
///
/// Se désactive dans ⚙ Réglages.
/// </summary>
internal static class MajJournal
{
    /// <summary>
    /// L'adresse publique du journal. À RENSEIGNER une fois le dépôt en ligne, par
    /// exemple https://tibs971.github.io/journal-phasmo/index.html
    /// Tant qu'elle est vide, la vérification ne part pas : l'application reste
    /// entièrement hors ligne.
    /// </summary>
    public const string Adresse = "";

    private static readonly string Dossier = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "PhasmoCompanion");

    /// <summary>Le journal téléchargé, s'il y en a un.</summary>
    public static string FichierTelecharge => Path.Combine(Dossier, "journal-telecharge.html");

    private static string FichierReglage => Path.Combine(Dossier, "maj.txt");

    /// <summary>Un journal téléchargé plus récent que l'embarqué attend d'être appliqué.</summary>
    public static bool MiseAJourEnAttente { get; private set; }

    // ---------- le réglage ----------

    public static bool Active
    {
        get
        {
            try { if (File.Exists(FichierReglage)) return File.ReadAllText(FichierReglage).Trim() != "0"; }
            catch { }
            return true;                                   // activée par défaut
        }
        set
        {
            try
            {
                Directory.CreateDirectory(Dossier);
                File.WriteAllText(FichierReglage, value ? "1" : "0");
            }
            catch { }
        }
    }

    private static DateTime DerniereVerif
    {
        get
        {
            try
            {
                var f = Path.Combine(Dossier, "maj-date.txt");
                if (File.Exists(f) && DateTime.TryParse(File.ReadAllText(f), out var d)) return d;
            }
            catch { }
            return DateTime.MinValue;
        }
        set
        {
            try
            {
                Directory.CreateDirectory(Dossier);
                File.WriteAllText(Path.Combine(Dossier, "maj-date.txt"), value.ToString("o"));
            }
            catch { }
        }
    }

    // ---------- la vérification ----------

    /// <summary>
    /// Va voir s'il existe un journal plus récent. Ne bloque jamais le démarrage et
    /// n'échoue jamais bruyamment : sans réseau, l'application marche comme avant.
    /// </summary>
    /// <returns>vrai si un journal plus récent a été retenu</returns>
    public static async Task<bool> VerifierAsync(string journalActuel, bool forcer = false)
    {
        if (!forcer)
        {
            if (!Active) return false;
            if ((DateTime.UtcNow - DerniereVerif).TotalHours < 24) return false;   // une fois par jour suffit
        }
        if (string.IsNullOrWhiteSpace(Adresse)) return false;

        try
        {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
            client.DefaultRequestHeaders.Add("User-Agent", "JournalPhasmo");
            var recu = await client.GetStringAsync(Adresse).ConfigureAwait(false);

            DerniereVerif = DateTime.UtcNow;

            if (!EstUnJournalComplet(recu)) return false;        // tronqué, page d'erreur, redirection…
            if (recu == journalActuel) return false;             // déjà à jour

            Directory.CreateDirectory(Dossier);
            File.WriteAllText(FichierTelecharge, recu);
            MiseAJourEnAttente = true;
            return true;
        }
        catch
        {
            return false;      // pas de réseau, serveur muet, pare-feu : on n'en parle pas
        }
    }

    /// <summary>
    /// Le garde-fou : on ne remplace le journal que si ce qu'on a reçu EST le journal.
    /// Sans ça, une page d'erreur ou un portail wifi remplacerait l'application par
    /// une page blanche, et l'utilisateur n'aurait aucun moyen de revenir en arrière.
    /// </summary>
    public static bool EstUnJournalComplet(string? texte) =>
        texte is not null
        && texte.Length > 150_000
        && texte.Contains("<title>Journal Phasmo</title>", StringComparison.Ordinal)
        && texte.Contains("GHOSTS", StringComparison.Ordinal)
        && texte.TrimEnd().EndsWith("</html>", StringComparison.OrdinalIgnoreCase);

    /// <summary>Oublie le journal téléchargé et revient à celui embarqué dans l'exe.</summary>
    public static void Oublier()
    {
        try { if (File.Exists(FichierTelecharge)) File.Delete(FichierTelecharge); } catch { }
        MiseAJourEnAttente = false;
    }
}
