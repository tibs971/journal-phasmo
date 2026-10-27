using System.Windows;
using System.Windows.Input;

namespace PhasmoCompanion;

/// <summary>
/// Renomme les trois minuteurs. Un champ vide reprend le nom d'origine :
/// l'utilisateur n'a pas à retrouver le libellé exact pour revenir en arrière.
/// </summary>
public partial class RenameDialog : Window
{
    public const string Def1 = "Encens", Def2 = "Chasse", Def3 = "Recharge";

    public string Nom1 { get; private set; } = Def1;
    public string Nom2 { get; private set; } = Def2;
    public string Nom3 { get; private set; } = Def3;

    public RenameDialog(Window? proprietaire, string n1, string n2, string n3)
    {
        InitializeComponent();
        if (proprietaire is not null && proprietaire.IsVisible) Owner = proprietaire;
        N1.Text = n1; N2.Text = n2; N3.Text = n3;
        Loaded += (_, _) => { N1.Focus(); N1.SelectAll(); };
    }

    private static string Propre(string saisi, string defaut)
    {
        var t = (saisi ?? "").Trim();
        return t.Length == 0 ? defaut : t;
    }

    private void Ok_Click(object sender, RoutedEventArgs e)
    {
        Nom1 = Propre(N1.Text, Def1);
        Nom2 = Propre(N2.Text, Def2);
        Nom3 = Propre(N3.Text, Def3);
        DialogResult = true;
    }

    private void Defaut_Click(object sender, RoutedEventArgs e)
    {
        N1.Text = Def1; N2.Text = Def2; N3.Text = Def3;
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) => DialogResult = false;

    private void Champ_KeyDown(object sender, KeyEventArgs e)
    {
        if (e.Key == Key.Escape) { DialogResult = false; e.Handled = true; }
    }

    private void Drag(object sender, MouseButtonEventArgs e)
    {
        if (e.ButtonState == MouseButtonState.Pressed) DragMove();
    }
}
