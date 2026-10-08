using System;
using System.Windows;
using System.Windows.Input;

namespace PhasmoCompanion;

/// <summary>Fenêtre finale : qui a fait l'appli, quand, avec quoi, et son empreinte.</summary>
public partial class CertificateWindow : Window
{
    private const string Outil =
        "Claude (Anthropic) — modèle claude-opus-5, mode Cowork";

    public CertificateWindow(Window? proprietaire)
    {
        InitializeComponent();
        if (proprietaire is not null && proprietaire.IsVisible) Owner = proprietaire;

        LblAuteur.Text = Signature.Auteur;
        LblCreation.Text = Signature.DateCreation;
        LblBuild.Text = Signature.DateCompilation();
        LblVersion.Text = "Journal Phasmo " + Signature.VersionAssemblage;
        LblOutil.Text = Outil;
        LblTech.Text = ".NET " + Signature.VersionDotNet + " · WPF · WebView2 " + Signature.VersionWebView2;
        LblEmpreinte.Text = Signature.EmpreinteExe();
    }

    private string Texte() =>
        "CERTIFICAT D'AUTEUR — Journal Phasmo\r\n" +
        "Auteur            : " + Signature.Auteur + "\r\n" +
        "Créée le          : " + Signature.DateCreation + "\r\n" +
        "Cet exécutable    : " + Signature.DateCompilation() + "\r\n" +
        "Version           : Journal Phasmo " + Signature.VersionAssemblage + "\r\n" +
        "Conçue avec       : " + Outil + "\r\n" +
        "Construite sur    : .NET " + Signature.VersionDotNet + " · WPF · WebView2 " + Signature.VersionWebView2 + "\r\n" +
        "Données du jeu    : Phasmophobia v0.19 — 30 entités, 18 cartes\r\n" +
        "Empreinte SHA-256 : " + Signature.EmpreinteExe().Replace("\n", " ").Replace(" ", "") + "\r\n" +
        "Relevé le         : " + DateTime.Now.ToString("dd/MM/yyyy HH:mm") + "\r\n";

    private void Copier_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            Clipboard.SetText(Texte());
            BtnCopier.Content = "Copié ✓";
        }
        catch { BtnCopier.Content = "Copie refusée"; }
    }

    private void Close_Click(object sender, RoutedEventArgs e) => Close();

    private void Drag(object sender, MouseButtonEventArgs e)
    {
        if (e.ButtonState == MouseButtonState.Pressed) DragMove();
    }
}
