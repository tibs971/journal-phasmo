package io.github.tibs971.journalphasmo

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.R as MatR
import com.google.android.material.tabs.TabLayout

/**
 * Le guide : difficultés, équipement, cartes, objets maudits, idées reçues.
 * Tout sort de donnees.json, rien n'est écrit ici.
 *
 * Quelques champs de la table des difficultés ne sont pas affichés : `gs`, `ps`, `pre`,
 * `free`, `sure` et `lvl`. Je ne suis pas certain de ce qu'ils désignent, et je préfère
 * ne rien montrer plutôt que de leur coller un libellé inventé.
 */
class EcranGuide(private val act: MainActivity) {

    private val d = act.donnees

    private val rubriques = listOf(
        "Difficultés", "Équipement", "Cartes", "Objets maudits", "Idées reçues"
    )

    private lateinit var contenu: LinearLayout
    private lateinit var defilement: ScrollView
    private var position = 0
    private var recherche = ""

    fun vue(): View {
        val racine = LayoutInflater.from(act).inflate(R.layout.ecran_guide, null, false)
        val onglets: TabLayout = racine.findViewById(R.id.onglets)
        contenu = racine.findViewById(R.id.contenuGuide)
        defilement = racine.findViewById(R.id.defilement)

        rubriques.forEach { onglets.addTab(onglets.newTab().setText(it)) }
        onglets.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(onglet: TabLayout.Tab) {
                position = onglet.position
                remplir()
            }
            override fun onTabUnselected(onglet: TabLayout.Tab) {}
            override fun onTabReselected(onglet: TabLayout.Tab) {}
        })
        brancherRecherche(racine, "Rechercher dans cette rubrique") {
            recherche = it
            remplir()
        }
        remplir()
        return racine
    }

    private fun remplir() {
        contenu.removeAllViews()
        defilement.scrollTo(0, 0)
        when (position) {
            0 -> difficultes()
            1 -> equipement()
            2 -> cartes()
            3 -> maudits()
            else -> idees()
        }
        if (contenu.childCount == 0) {
            contenu.addView(
                act.paragraphe("Rien ne correspond à « $recherche » dans cette rubrique.", pale = true)
            )
        }
    }

    /** une fiche n'est gardée que si la recherche se retrouve quelque part dedans */
    private fun garder(vararg morceaux: Any?): Boolean {
        if (recherche.isBlank()) return true
        for (m in morceaux) {
            when (m) {
                is String -> if (contient(m, recherche)) return true
                is List<*> -> for (x in m) if (contient(x?.toString(), recherche)) return true
                null -> {}
                else -> if (contient(m.toString(), recherche)) return true
            }
        }
        return false
    }

    // ---------- les cinq rubriques ----------

    private fun difficultes() {
        for (diff in d.difficultes) {
            if (!garder(diff["n"], diff["note"], diff["ins"])) continue
            val c = act.colonne().apply { setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12)) }
            c.addView(titre(txt(diff["n"])))
            ligne(c, "Preuves", nb(diff["ev"]) + (if (nb(diff["hid"]).toInt() > 0) "  ·  ${nb(diff["hid"])} cachée(s)" else ""))
            ligne(c, "Préparation", nb(diff["setup"]) + " s")
            ligne(c, "Santé au départ", nb(diff["san0"]) + " %")
            ligne(c, "Perte de santé", nb(diff["drain"]) + " %")
            ligne(c, "Gain d'une pilule", nb(diff["pill"]) + " %")
            ligne(c, "Période de grâce", nb(diff["grace"]) + " s")
            (diff["hunt"] as? List<*>)?.let { h ->
                val tailles = d.taillesCarte.map { txt(it["n"]) }
                ligne(c, "Durée de chasse", h.mapIndexed { i, v ->
                    (tailles.getOrNull(i) ?: "?") + " " + nbBrut(v) + " s"
                }.joinToString("  ·  "))
            }
            ligne(c, "Objets maudits", nb(diff["cursed"]))
            ligne(c, "Empreintes", nb(diff["fing"]) + " s")
            ligne(c, "Assurance", txt(diff["ins"]))
            txt(diff["note"]).takeIf { it.isNotBlank() }?.let {
                c.addView(act.paragraphe(it, pale = true, petit = true))
            }
            contenu.addView(carte(c))
        }
    }

    private fun equipement() {
        for (eq in d.equipement) {
            if (!garder(eq["n"], eq["d"], eq["note"], eq["t"])) continue
            val c = act.colonne().apply { setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12)) }
            c.addView(titre(txt(eq["n"])))
            c.addView(act.paragraphe(FicheEntite.depouiller(txt(eq["d"])), petit = true))
            (eq["t"] as? List<*>)?.forEachIndexed { i, rang ->
                val chiffre = listOf("I", "II", "III").getOrNull(i) ?: (i + 1).toString()
                ligne(c, "Rang $chiffre", FicheEntite.depouiller(rang?.toString() ?: ""))
            }
            txt(eq["note"]).takeIf { it.isNotBlank() }?.let {
                c.addView(act.paragraphe(FicheEntite.depouiller(it), pale = true, petit = true))
            }
            contenu.addView(carte(c))
        }
    }

    private fun cartes() {
        // les cartes sont groupées par taille, et chaque taille porte ses propres chiffres
        d.taillesCarte.forEachIndexed { indice, taille ->
            val liste = d.cartes
                .filter { (it["s"] as? Double)?.toInt() == indice }
                .filter { garder(it["n"], taille["n"]) }
            if (liste.isEmpty()) return@forEachIndexed

            contenu.addView(act.etiquette(txt(taille["n"])))
            (taille["drain"] as? List<*>)?.let {
                contenu.addView(act.paragraphe(
                    "Perte de santé : " + it.joinToString(" à ") { v -> nbBrut(v) } + " % par seconde",
                    pale = true, petit = true))
            }
            for (carte in liste) contenu.addView(act.paragraphe("·  " + txt(carte["n"]), petit = true))
        }
    }

    private fun maudits() {
        for (obj in d.objetsMaudits) {
            if (!garder(obj["n"], obj["how"], obj["risk"], obj["list"])) continue
            val c = act.colonne().apply { setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12)) }
            c.addView(titre(txt(obj["n"])))
            c.addView(act.paragraphe(FicheEntite.depouiller(txt(obj["how"])), petit = true))
            (obj["list"] as? List<*>)?.forEach {
                c.addView(act.paragraphe("·  " + FicheEntite.depouiller(it?.toString() ?: ""), petit = true))
            }
            txt(obj["risk"]).takeIf { it.isNotBlank() }?.let {
                c.addView(act.paragraphe("Risque : " + FicheEntite.depouiller(it), petit = true).apply {
                    setTextColor(act.couleur(R.color.jp_signalee))
                })
            }
            contenu.addView(carte(c))
        }
    }

    private fun idees() {
        for (idee in d.ideesRecues) {
            if (!garder(idee)) continue
            val c = act.colonne().apply { setPadding(act.dp(14), act.dp(12), act.dp(14), act.dp(12)) }
            c.addView(titre(FicheEntite.depouiller(idee.getOrElse(0) { "" })))
            c.addView(act.paragraphe(FicheEntite.depouiller(idee.getOrElse(1) { "" }), petit = true))
            contenu.addView(carte(c))
        }
    }

    // ---------- petits assembleurs ----------

    private fun titre(t: String) = act.paragraphe(t).apply {
        setTextAppearance(MatR.style.TextAppearance_Material3_TitleMedium)
        setTextColor(act.couleurTheme(MatR.attr.colorOnSurface))
    }

    /** un libellé en texte, sa valeur en largeur fixe : les chiffres s'alignent */
    private fun ligne(parent: LinearLayout, nom: String, valeur: String) {
        if (valeur.isBlank()) return
        val r = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        r.addView(act.paragraphe(nom, pale = true, petit = true),
            LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(act.releve(valeur).apply {
            setTextAppearance(R.style.Releve)
            setTextColor(act.couleurTheme(MatR.attr.colorOnSurface))
        })
        parent.addView(r)
    }

    /**
     * Un bloc du guide : pas de carte. Un filet suffit à séparer, et il n'ajoute pas
     * un cadre de plus à une page qui en compterait déjà vingt.
     */
    private fun carte(dedans: View): View {
        val bloc = act.colonne()
        bloc.addView(dedans)
        bloc.addView(act.filet(act.dp(6)))
        return bloc
    }

    private fun txt(v: Any?): String = v as? String ?: ""

    /** les nombres du JSON sont des Double : 50.0 doit s'afficher « 50 » */
    private fun nb(v: Any?): String = nbBrut(v)

    private fun nbBrut(v: Any?): String {
        val x = v as? Double ?: return v?.toString() ?: ""
        return if (x == Math.floor(x)) x.toLong().toString()
        else x.toString().replace('.', ',')
    }
}
