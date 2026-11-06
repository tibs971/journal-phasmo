package io.github.tibs971.journalphasmo

import android.os.SystemClock
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.R as MatR
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Les minuteurs : quatre visibles, deux à gauche et deux à droite en portrait, et une
 * seule rangée de quatre en paysage — le téléphone est posé à côté du clavier.
 *
 * Chaque minuteur s'affiche au choix en minutes:secondes ou en secondes seules. Les
 * secondes seules servent pour tout ce qui se compte en dizaines de secondes et se
 * compare mentalement : 300 s se lit d'un coup, 5:00 demande une conversion.
 *
 * Un appui long sur un minuteur le renomme. Le nom est gardé d'une fois sur l'autre.
 */
class EcranChasse(private val act: MainActivity) {

    private val d = act.donnees
    private val m = act.moteur
    private val e = act.etat

    private inner class Minuteur(val cle: String, val defaut: String) {
        var cumul = 0L
        var depart = 0L
        var enMarche = false
        var enSecondes = Sauvegarde.minuteurEnSecondes(act, cle)
        lateinit var titre: TextView
        lateinit var affichage: TextView
        lateinit var bouton: MaterialButton
        lateinit var unite: MaterialButton

        fun ecoule(): Long =
            cumul + if (enMarche) SystemClock.elapsedRealtime() - depart else 0L

        fun majAffichage() {
            val total = ecoule() / 1000
            affichage.text = if (enSecondes) "$total s"
            else String.format("%d:%02d", total / 60, total % 60)
            unite.text = if (enSecondes) "s" else "m:s"
        }
    }

    private val minuteurs = ArrayList<Minuteur>()

    fun vue(): View {
        val corps = act.colonne().apply { setPadding(act.dp(12), act.dp(12), act.dp(12), act.dp(24)) }
        val paysage = act.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val cles = listOf(
            "smudge" to (d.libellesRaccourcis["smudge"] ?: "Encens"),
            "hunt" to (d.libellesRaccourcis["hunt"] ?: "Chasse"),
            "cd" to (d.libellesRaccourcis["cd"] ?: "Recharge"),
            "sec" to (d.libellesRaccourcis["sec"] ?: "Chrono")
        )

        corps.addView(act.etiquette("Minuteurs"))
        corps.addView(
            act.paragraphe("Appui long sur un minuteur pour le renommer.", pale = true, petit = true)
        )

        minuteurs.clear()
        if (paysage) {
            val rangee = rangee()
            cles.forEach { rangee.addView(carteMinuteur(it.first, it.second), poids()) }
            corps.addView(rangee)
        } else {
            for (paire in cles.chunked(2)) {
                val rangee = rangee()
                paire.forEach { rangee.addView(carteMinuteur(it.first, it.second), poids()) }
                corps.addView(rangee)
            }
        }

        val restantes = d.entites.filter { m.raison(it, e) == null }
        corps.addView(act.etiquette("Vitesses encore possibles — ${restantes.size}"))
        if (restantes.isEmpty()) {
            corps.addView(act.paragraphe("Aucune entité ne colle : revoyez une observation.", pale = true))
        } else for (ent in restantes) {
            val r = rangee()
            r.addView(act.paragraphe(ent.fr, petit = true),
                LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            r.addView(act.releve(ent.vit).apply {
                setTextColor(act.couleurTheme(MatR.attr.colorOnSurfaceVariant))
            })
            corps.addView(r)
            corps.addView(act.filet(act.dp(4)))
        }

        val racine = ScrollView(act).apply { addView(corps) }
        racine.post { battre(racine) }
        return racine
    }

    /** un battement toutes les 200 ms, qui s'arrête de lui-même quand l'écran disparaît */
    private fun battre(racine: View) {
        if (!racine.isAttachedToWindow) return
        for (min in minuteurs) min.majAffichage()
        racine.postDelayed({ battre(racine) }, 200)
    }

    private fun carteMinuteur(cle: String, defaut: String): View {
        val min = Minuteur(cle, defaut)
        minuteurs.add(min)

        val dedans = act.colonne().apply { setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10)) }

        min.titre = act.paragraphe(Sauvegarde.nomMinuteur(act, cle, defaut), pale = true, petit = true)
        dedans.addView(min.titre)

        // l'afficheur : en largeur fixe, pour que le chiffre ne danse pas a chaque seconde
        min.affichage = act.releve("0:00", grand = true).apply {
            setTextColor(act.couleurTheme(MatR.attr.colorOnSurface))
        }
        dedans.addView(min.affichage)

        val boutons = rangee()

        min.bouton = MaterialButton(act).apply {
            text = "Départ"
            setOnClickListener {
                if (min.enMarche) {
                    min.cumul = min.ecoule()
                    min.enMarche = false
                    text = "Reprendre"
                } else {
                    min.depart = SystemClock.elapsedRealtime()
                    min.enMarche = true
                    text = "Pause"
                }
                min.majAffichage()
            }
        }
        boutons.addView(min.bouton, poidsSimple())

        // bascule minutes:secondes <-> secondes seules
        min.unite = MaterialButton(act, null, MatR.attr.materialButtonOutlinedStyle).apply {
            setOnClickListener {
                min.enSecondes = !min.enSecondes
                Sauvegarde.basculerUnite(act, cle, min.enSecondes)
                min.majAffichage()
            }
        }
        boutons.addView(min.unite, poidsSimple())
        dedans.addView(boutons)

        val zero = act.paragraphe("Remise à 0", petit = true).apply {
            setTextColor(act.couleurTheme(MatR.attr.colorPrimary))
            minHeight = act.dp(44)
            isClickable = true
            setOnClickListener {
                min.enMarche = false
                min.cumul = 0L
                min.bouton.text = "Départ"
                min.majAffichage()
            }
        }
        dedans.addView(zero)

        min.majAffichage()

        // Un afficheur, pas une carte : fond plein et angles droits. C'est ce qui le
        // distingue du reste de l'application, qui n'a aucun cadre.
        return LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(act.couleurTheme(MatR.attr.colorSurfaceVariant))
            addView(dedans)
            isLongClickable = true
            setOnLongClickListener { renommer(min); true }
        }
    }

    private fun renommer(min: Minuteur) {
        val champ = EditText(act).apply {
            setText(min.titre.text)
            setSelection(text.length)
            setPadding(act.dp(20), act.dp(12), act.dp(20), act.dp(12))
        }
        MaterialAlertDialogBuilder(act)
            .setTitle("Renommer ce minuteur")
            .setView(champ)
            .setPositiveButton("Garder") { _, _ ->
                val nom = champ.text.toString().trim().ifEmpty { min.defaut }
                Sauvegarde.renommerMinuteur(act, min.cle, nom)
                min.titre.text = nom
            }
            .setNeutralButton("Nom d'origine") { _, _ ->
                Sauvegarde.renommerMinuteur(act, min.cle, min.defaut)
                min.titre.text = min.defaut
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun rangee() = LinearLayout(act).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun poids() = LinearLayout.LayoutParams(
        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f
    ).apply { setMargins(act.dp(4), act.dp(4), act.dp(4), act.dp(4)) }

    private fun poidsSimple() = LinearLayout.LayoutParams(
        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f
    ).apply { setMargins(act.dp(2), 0, act.dp(2), 0) }
}
