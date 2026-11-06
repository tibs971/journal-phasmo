package io.github.tibs971.journalphasmo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.R as MatR
import com.google.android.material.button.MaterialButton

/**
 * Les 30 entités — et surtout celles qui tombent.
 *
 * Le sujet de l'application, c'est l'élimination : rayer 29 noms sur 30. Donc les noms
 * écartés sont réellement barrés, comme sur un carnet, et la raison est écrite à côté.
 * Aucune carte : les lignes sont séparées par un filet, et l'entité désignée porte un
 * trait d'accent sur sa gauche. La hiérarchie vient de la structure, pas de boîtes.
 */
class EcranEntites(private val act: MainActivity) {

    private val d = act.donnees
    private val m = act.moteur
    private val e = act.etat

    private lateinit var liste: LinearLayout
    private var recherche = ""

    fun vue(): View {
        val racine = act.colonne()
        LayoutInflater.from(act).inflate(R.layout.barre_recherche, racine, true)

        liste = act.colonne().apply { setPadding(0, 0, 0, act.dp(28)) }
        racine.addView(ScrollView(act).apply {
            addView(liste)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        brancherRecherche(racine, "Rechercher une entité, une preuve, un indice") {
            recherche = it
            remplir()
        }
        remplir()
        return racine
    }

    private fun correspond(ent: Entite): Boolean {
        if (recherche.isBlank()) return true
        val tout = listOf(ent.fr, ent.en, ent.vit, ent.seuil, ent.key) +
            ent.ev.map { d.preuves[it]?.fr ?: it } +
            ent.capa + ent.indices + ent.confirm + ent.elim
        return tout.any { contient(it, recherche) }
    }

    private fun remplir() {
        liste.removeAllViews()
        val toutes = d.entites.filter { correspond(it) }
        val restantes = toutes.filter { m.raison(it, e) == null }
        val ecartees = toutes.filter { m.raison(it, e) != null }

        if (toutes.isEmpty()) {
            liste.addView(act.paragraphe("Rien ne correspond à « $recherche ».", pale = true).apply {
                setPadding(act.dp(16), act.dp(20), act.dp(16), 0)
            })
            return
        }

        e.choisie?.let { id ->
            d.entites.firstOrNull { it.id == id }?.let { ent ->
                liste.addView(titreSection("Votre réponse"))
                liste.addView(ligne(ent, m.raison(ent, e)))
            }
        }
        if (restantes.isNotEmpty()) {
            liste.addView(titreSection(
                if (restantes.size == 1) "Une seule tient encore" else "${restantes.size} tiennent encore"
            ))
            for (ent in restantes) liste.addView(ligne(ent, null))
        }
        if (ecartees.isNotEmpty()) {
            liste.addView(titreSection("${ecartees.size} écartées"))
            for (ent in ecartees) liste.addView(ligne(ent, m.raison(ent, e)))
        }
    }

    private fun titreSection(t: String) = act.etiquette(t).apply {
        setPadding(act.dp(16), act.dp(22), act.dp(16), act.dp(8))
    }

    private fun ligne(ent: Entite, raison: String?): View {
        val ecartee = raison != null
        val choisie = e.choisie == ent.id

        val texte = act.colonne().apply {
            setPadding(act.dp(14), act.dp(12), act.dp(16), act.dp(10))
        }

        texte.addView(act.paragraphe(ent.fr).apply {
            setTextAppearance(R.style.Texte_Titre)
            setTextColor(act.couleurTheme(if (ecartee) MatR.attr.colorOnSurfaceVariant else MatR.attr.colorOnSurface))
            barrer(ecartee)
        })

        // les valeurs mesurées, en largeur fixe : deux entités alignées se comparent
        texte.addView(act.releve(ent.vit).apply {
            setTextColor(act.couleurTheme(MatR.attr.colorOnSurfaceVariant))
        })
        texte.addView(act.paragraphe("Chasse sous ${ent.seuil} de santé moyenne", pale = true, petit = true))
        texte.addView(act.paragraphe(
            ent.ev.joinToString("   ") { d.preuves[it]?.fr ?: it }, pale = true, petit = true))

        if (raison != null) texte.addView(
            act.paragraphe(raison.replaceFirstChar { it.uppercase() }, petit = true).apply {
                setTextColor(act.couleurTheme(MatR.attr.colorPrimary))
            }
        )

        val actions = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        fun poids() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(0, act.dp(8), act.dp(8), 0) }

        actions.addView(MaterialButton(act, null, MatR.attr.materialButtonOutlinedStyle).apply {
            text = "Fiche"
            setOnClickListener { FicheEntite.afficher(act, d, ent, raison) { choisir(ent) } }
        }, poids())
        actions.addView(MaterialButton(act, null, MatR.attr.materialButtonOutlinedStyle).apply {
            text = if (choisie) "C'est bien elle" else "C'est celle-là"
            setOnClickListener { choisir(ent) }
        }, poids())
        texte.addView(actions)

        if (e.ecarteesAlaMain.contains(ent.id)) {
            texte.addView(act.paragraphe("Remettre dans la liste", petit = true).apply {
                setTextColor(act.couleurTheme(MatR.attr.colorPrimary))
                minHeight = act.dp(48)
                isClickable = true
                setOnClickListener {
                    e.ecarteesAlaMain.remove(ent.id)
                    act.etatModifie(); remplir()
                }
            })
        }

        // le trait d'accent à gauche désigne votre réponse : un repère de structure,
        // pas un encadré de plus
        val rangee = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(View(act).apply {
                setBackgroundColor(
                    if (choisie) act.couleurTheme(MatR.attr.colorPrimary)
                    else android.graphics.Color.TRANSPARENT
                )
                layoutParams = LinearLayout.LayoutParams(act.dp(3), ViewGroup.LayoutParams.MATCH_PARENT)
            })
            addView(texte, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        val bloc = act.colonne()
        bloc.addView(rangee)
        bloc.addView(act.filet())
        return bloc
    }

    private fun choisir(ent: Entite) {
        e.choisie = if (e.choisie == ent.id) null else ent.id
        act.etatModifie()
        remplir()
    }
}
