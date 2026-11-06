package io.github.tibs971.journalphasmo

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView

/**
 * La coquille : quatre destinations, et l'état partagé de l'enquête.
 *
 * Pourquoi une barre basse et non un menu à trois traits : la doc de Google dit
 * « The navigation drawer is being deprecated in the Material 3 expressive update »,
 * et son remplaçant — le rail étendu — n'existe que sur grand écran. Surtout, un menu
 * coûte deux gestes là où la barre en coûte un, et on s'en sert une main sur le clavier.
 *
 * En paysage, la mise en page est remplacée par `layout-land`, qui met un rail sur le
 * côté : même menu, même code, l'identifiant `nav` désigne les deux.
 */
class MainActivity : AppCompatActivity() {

    lateinit var donnees: Donnees
        private set
    lateinit var moteur: Moteur
        private set
    val etat = Etat()

    private var destination = R.id.dest_enquete

    override fun onCreate(etatSauve: Bundle?) {
        super.onCreate(etatSauve)
        // la palette choisie, posée avant toute mise en page
        setTheme(styleDuTheme(Sauvegarde.theme(this)))

        try {
            val texte = assets.open("donnees.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            donnees = Donnees.depuis(texte)
        } catch (e: Exception) {
            setContentView(TextView(this).apply {
                text = "Le journal n'a pas pu charger ses données.\n\n${e.message}"
                setPadding(dp(24), dp(64), dp(24), dp(24))
                setTextColor(couleurTheme(com.google.android.material.R.attr.colorOnSurface))
            })
            return
        }
        moteur = Moteur(donnees)
        donnees.ordrePreuves.forEach { etat.preuves[it] = 0 }
        Sauvegarde.lire(this, etat)

        if (etatSauve != null) destination = etatSauve.getInt("destination", R.id.dest_enquete)

        setContentView(R.layout.activity_main)
        contournerLesBarres()
        reglerLesIconesSysteme()

        findViewById<MaterialToolbar>(R.id.barre)?.let { barre ->
            barre.inflateMenu(R.menu.actions)
            barre.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.act_zero -> { demanderRemiseAZero(); true }
                    R.id.act_theme -> { choisirTheme(); true }
                    else -> false
                }
            }
        }

        val nav = findViewById<NavigationBarView>(R.id.nav)
        nav.selectedItemId = destination
        nav.setOnItemSelectedListener { item ->
            destination = item.itemId
            afficher()
            true
        }
        afficher()
    }

    override fun onSaveInstanceState(sortie: Bundle) {
        super.onSaveInstanceState(sortie)
        sortie.putInt("destination", destination)
    }

    override fun onPause() {
        super.onPause()
        if (this::donnees.isInitialized) Sauvegarde.ecrire(this, etat)
    }

    /** à appeler dès qu'une case change : l'état est gardé tout de suite */
    fun etatModifie() = Sauvegarde.ecrire(this, etat)

    /** redessine la destination courante — utile quand un écran modifie l'enquête */
    fun recharger() = afficher()

    // ---------- thème et remise à zéro ----------

    private val themes = listOf(
        // ce n'est pas un cinquième thème : c'est Ardoise ou Clair, choisi par le téléphone
        "auto" to "Automatique — Ardoise la nuit, Clair le jour",
        "ardoise" to "Ardoise",
        "clair" to "Clair",
        "carnet" to "Carnet",
        "nocturne" to "Vision nocturne"
    )

    private fun styleDuTheme(cle: String): Int = when (cle) {
        "ardoise" -> R.style.Theme_JournalPhasmo_Ardoise
        "clair" -> R.style.Theme_JournalPhasmo_Clair
        "carnet" -> R.style.Theme_JournalPhasmo_Carnet
        "nocturne" -> R.style.Theme_JournalPhasmo_Nocturne
        else -> R.style.Theme_JournalPhasmo
    }

    private fun choisirTheme() {
        val actuel = Sauvegarde.theme(this)
        var choix = themes.indexOfFirst { it.first == actuel }.coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("Thème")
            .setSingleChoiceItems(themes.map { it.second }.toTypedArray(), choix) { _, i -> choix = i }
            .setPositiveButton("Appliquer") { _, _ ->
                val cle = themes[choix].first
                if (cle != actuel) {
                    Sauvegarde.definirTheme(this, cle)
                    recreate()          // la palette se pose à la création de l'écran
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun demanderRemiseAZero() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Nouvelle enquête")
            .setMessage(
                "Tout ce qui est coché sera effacé : les preuves, les vitesses, les " +
                    "observations, les entités écartées à la main et votre réponse.\n\n" +
                    "Votre thème et les noms donnés aux minuteurs sont conservés."
            )
            .setPositiveButton("Tout effacer") { _, _ ->
                Sauvegarde.effacer(this, etat)
                donnees.ordrePreuves.forEach { etat.preuves[it] = 0 }
                afficher()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /** les icônes de la barre d'état doivent contraster avec le fond du thème */
    private fun reglerLesIconesSysteme() {
        val racine = findViewById<View>(R.id.racine) ?: return
        val t = android.util.TypedValue()
        val clair = theme.resolveAttribute(android.R.attr.isLightTheme, t, true) && t.data != 0
        WindowCompat.getInsetsController(window, racine).apply {
            isAppearanceLightStatusBars = clair
            isAppearanceLightNavigationBars = clair
        }
    }

    /**
     * Depuis qu'on cible une version récente d'Android, le système dessine l'application
     * SOUS la barre de notification et sous les boutons du téléphone. Sans cette marge,
     * les onglets passent derrière l'heure en haut, et en paysage la dernière ligne
     * disparaît derrière les boutons de navigation.
     *
     * On prend aussi en compte l'encoche : sur un téléphone à encoche en paysage, elle
     * mange le bord de l'écran du côté où elle se trouve.
     */
    private fun contournerLesBarres() {
        val racine = findViewById<View>(R.id.racine) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(racine) { vue, marges ->
            val b = marges.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            vue.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(racine)
    }

    private fun afficher() {
        val boite = findViewById<FrameLayout>(R.id.contenu) ?: return
        boite.removeAllViews()
        val vue = when (destination) {
            R.id.dest_entites -> EcranEntites(this).vue()
            R.id.dest_chasse -> EcranChasse(this).vue()
            R.id.dest_guide -> EcranGuide(this).vue()
            else -> EcranEnquete(this).vue()
        }
        boite.addView(
            vue,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }
}
