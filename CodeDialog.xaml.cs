using System;
using System.Windows;
using System.Windows.Input;

namespace PhasmoCompanion;

/// <summary>
/// Petite fenêtre de saisie d'un code, masqué par défaut, révélable avec l'œil.
/// Sert pour les deux étapes : le texte et le test de validité sont passés au constructeur.
/// </summary>
public partial class CodeDialog : Window
{
    private readonly Func<string, bool> _verifie;
    private bool _visible;
    private bool _sync;          // évite que les deux champs se renvoient leurs changements
    private int _essais;

    private const int MaxEssais = 3;

    public CodeDialog(Window? proprietaire, string etape, string titre, string sousTitre,
                      Func<string, bool> verifie)
    {
        InitializeComponent();
        if (proprietaire is not null && proprietaire.IsVisible) Owner = proprietaire;
        _verifie = verifie;
        Etape.Text = etape;
        Titre.Text = titre;
        SousTitre.Text = sousTitre;
        MajEssais();
        Loaded += (_, _) => Masque.Focus();
    }

    private void MajEssais() =>
        Essais.Text = _essais == 0 ? "" : (MaxEssais - _essais) + " essai" + (MaxEssais - _essais > 1 ? "s" : "") + " restant" + (MaxEssais - _essais > 1 ? "s" : "");

    // ---------- œil : bascule entre champ masqué et champ lisible ----------
    private void Oeil_Click(object sender, RoutedEventArgs e)
    {
        _visible = !_visible;
        _sync = true;
        if (_visible)
        {
            Clair.Text = Masque.Password;
            Masque.Visibility = Visibility.Collapsed;
            Clair.Visibility = Visibility.Visible;
            Clair.Focus();
            Clair.CaretIndex = Clair.Text.Length;
            BtnOeil.Content = "🙈";
            BtnOeil.ToolTip = "Masquer le code";
        }
        else
        {
            Masque.Password = Clair.Text;
            Clair.Visibility = Visibility.Collapsed;
            Masque.Visibility = Visibility.Visible;
            Masque.Focus();
            BtnOeil.Content = "👁";
            BtnOeil.ToolTip = "Afficher le code";
        }
        _sync = false;
    }

    private void Masque_Changed(object sender, RoutedEventArgs e)
    {
        if (_sync) return;
        Erreur.Visibility = Visibility.Collapsed;
    }

    private void Clair_Changed(object sender, System.Windows.Controls.TextChangedEventArgs e)
    {
        if (_sync) return;
        Erreur.Visibility = Visibility.Collapsed;
    }

    private string Saisie => _visible ? Clair.Text : Masque.Password;

    private void Champ_KeyDown(object sender, KeyEventArgs e)
    {
        // Échap est géré par le bouton Annuler (IsCancel), ne pas le doubler ici
        if (e.Key == Key.Enter) { Valider(); e.Handled = true; }
    }

    private void Ok_Click(object sender, RoutedEventArgs e) => Valider();

    private void Valider()
    {
        if (_verifie(Saisie)) { DialogResult = true; return; }

        _essais++;
        Erreur.Text = "Code incorrect.";
        Erreur.Visibility = Visibility.Visible;
        MajEssais();

        if (_essais >= MaxEssais) { DialogResult = false; return; }

        _sync = true;
        Masque.Password = "";
        Clair.Text = "";
        _sync = false;
        if (_visible) Clair.Focus(); else Masque.Focus();
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) => DialogResult = false;

    private void Drag(object sender, MouseButtonEventArgs e)
    {
        if (e.ButtonState == MouseButtonState.Pressed) DragMove();
    }
}
