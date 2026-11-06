package io.github.tibs971.journalphasmo

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * L'écran d'enquête — le coeur de l'application, et ce qui la distingue : les 98
 * observations vérifiées, les paliers de vitesse, les vitesses de départ et le mode
 * « resté caché ». Les trois autres destinations restent volontairement sobres.
 *
 * Le bandeau du haut ne défile pas : pendant qu'on parcourt les observations, le nombre
 * d'entités restantes reste sous les yeux. C'est le geste réel pendant une partie — on
 * jette un oeil à l'écran du téléphone, on ne le lit pas.
 */
class EcranEnquete(private val act: MainActivity) {

    private val d = act.donnees
    private val m = act.moteur
    private val e = act.etat

    private lateinit var txtRestantes: TextView
    private lateinit var chipsRestantes: ChipGroup
    private lateinit var chipsPreuves: ChipGroup
    private lateinit var chipsVitesses: ChipGroup
    private lateinit var zoneObs: LinearLayout
    private lateinit var chipCache: Chip
    private lateinit var noteCache: TextView
    private lateinit var racine: View
    private lateinit var defilRestantes: View
    private lateinit var lignesRestantes: LinearLayout
    private lateinit var zoneReprise: LinearLayout
    private var recherche = ""


    /** les groupes repliés survivent au passage sur une autre destination */
    companion object {
        private val replies = HashSet<String>()
    }

    fun vue(): View {
        racine = LayoutInflater.from(act).inflate(R.layout.ecran_enquete, null, false)

        txtRestantes = racine.findViewById(R.id.txtRestantes)
        chipsRestantes = racine.findViewById(R.id.chipsRestantes)
        defilRestantes = racine.findViewById(R.id.defilRestantes)
        lignesRestantes = racine.findViewById(R.id.lignesRestantes)
        zoneReprise = racine.findViewById(R.id.zoneReprise)
        chipsPreuves = racine.findViewById(R.id.chipsPreuves)
        chipsVitesses = racine.findViewById(R.id.chipsVitesses)
        zoneObs = racine.findViewById(R.id.zoneObs)
        chipCache = racine.findViewById(R.id.chipCache)
        noteCache = racine.findViewById(R.id.noteCache)

        construirePreuves()
        construireVitesses()
        construireNbPreuves(racine.findViewById(R.id.groupeNbPreuves))

        chipCache.isChecked = e.resteCache
        chipCache.setOnCheckedChangeListener { _, coche ->
            e.resteCache = coche
            // ce bouton change les sous-titres des observations : c'est le seul endroit
            // qui reconstruit la liste, sinon la vue sauterait à chaque case cochée
            construireObservations()
            rafraichir()
        }

        brancherRecherche(racine, "Rechercher dans les observations") {
            recherche = it
            construireObservations()
        }

        construireObservations()
        rafraichir()
        return racine
    }

    // ---------- les blocs ----------

    private fun puce(parent: ChipGroup): Chip {
        val c = LayoutInflater.from(act).inflate(R.layout.chip_etat, parent, false) as Chip
        parent.addView(c)
        return c
    }

    private fun construirePreuves() {
        chipsPreuves.removeAllViews()
        for (cle in d.ordrePreuves) {
            val c = puce(chipsPreuves)
            c.text = d.preuves[cle]!!.long
            c.tag = cle
            c.setOnClickListener {
                e.preuves[cle] = ((e.preuves[cle] ?: 0) + 1) % 3
                rafraichir()
            }
        }
    }

    private fun construireVitesses() {
        chipsVitesses.removeAllViews()
        for ((cle, nom) in d.paliers) {
            val c = puce(chipsVitesses)
            c.text = nom
            c.tag = cle
            c.setOnClickListener {
                val v = ((e.pastilles[cle] ?: 0) + 1) % 3
                if (v == 0) e.pastilles.remove(cle) else e.pastilles[cle] = v
                rafraichir()
            }
        }
    }

    private fun construireNbPreuves(groupe: MaterialButtonToggleGroup) {
        val ids = intArrayOf(R.id.nb0, R.id.nb1, R.id.nb2, R.id.nb3)
        groupe.check(ids[e.nbPreuves.coerceIn(0, 3)])
        groupe.addOnButtonCheckedListener { _, id, coche ->
            if (!coche) return@addOnButtonCheckedListener
            val n = ids.indexOf(id)
            if (n >= 0 && n != e.nbPreuves) {
                e.nbPreuves = n
                rafraichir()
            }
        }
    }

    private fun construireObservations() {
        zoneObs.removeAllViews()
        val inflater = LayoutInflater.from(act)
        var corps: LinearLayout? = null

        for (o in d.observations) {
            if (o.grp != null) {
                val nomGroupe = o.grp                       // capturé par itération
                // pendant une recherche, tous les groupes s'ouvrent : sinon le résultat
                // reste caché dans un groupe replié et on croit qu'il n'y a rien
                val replie = recherche.isBlank() && replies.contains(nomGroupe)

                val entete = inflater.inflate(R.layout.entete_groupe, zoneObs, false) as TextView
                entete.text = (if (replie) "▸   " else "▾   ") + nomGroupe

                val contenu = act.colonne().apply {
                    visibility = if (replie) View.GONE else View.VISIBLE
                    setPadding(act.dp(4), 0, 0, act.dp(10))
                }
                entete.setOnClickListener {
                    if (replies.contains(nomGroupe)) replies.remove(nomGroupe) else replies.add(nomGroupe)
                    construireObservations()
                }

                zoneObs.addView(act.paragraphe("", petit = true))   // un peu d'air
                zoneObs.addView(entete)
                zoneObs.addView(contenu)
                corps = contenu
                continue
            }

            val cible = corps ?: zoneObs

            if (o.info != null) {
                cible.addView(act.paragraphe(o.info, pale = true, petit = true))
                continue
            }
            if (!o.estLigne) continue
            if (!contient(o.t, recherche) && !contient(o.soustitre(e.resteCache), recherche)) continue

            val id = o.id!!
            val case = inflater.inflate(R.layout.case_obs, cible, false)
                    as com.google.android.material.checkbox.MaterialCheckBox
            case.text = o.t
            case.isChecked = e.cochees.contains(id)
            case.setOnClickListener {
                if (e.cochees.contains(id)) e.cochees.remove(id) else e.cochees.add(id)
                rafraichir()
            }
            cible.addView(case)

            o.soustitre(e.resteCache)?.let {
                cible.addView(act.paragraphe(it, pale = true, petit = true).apply {
                    setPadding(act.dp(40), 0, act.dp(8), act.dp(8))
                })
            }
            if (o.flag != null) {
                cible.addView(act.paragraphe(o.txt ?: "Signale sans éliminer", petit = true).apply {
                    setTextColor(act.couleur(R.color.jp_signalee))
                    setPadding(act.dp(40), 0, act.dp(8), act.dp(8))
                })
            }
        }
    }

    // ---------- mise à jour ----------

    /**
     * Une entité quand il n'en reste plus que quelques-unes : trois cibles distinctes.
     * La coche désigne, le nom ouvre la fiche, la croix écarte. Aucune des trois ne
     * déclenche les deux autres.
     */
    private fun ligneEntite(ent: Entite, signalees: Set<String>): View {
        val choisie = e.choisie == ent.id
        val rangee = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        rangee.addView(act.paragraphe(if (choisie) "✓" else "○").apply {
            setTextColor(act.couleurTheme(
                if (choisie) com.google.android.material.R.attr.colorPrimary
                else com.google.android.material.R.attr.colorOnSurfaceVariant))
            minWidth = act.dp(48); minHeight = act.dp(48)
            gravity = android.view.Gravity.CENTER
            isClickable = true
            contentDescription = if (choisie) "Ne plus désigner " + ent.fr else "Désigner " + ent.fr
            setOnClickListener {
                e.choisie = if (choisie) null else ent.id
                act.etatModifie(); rafraichir()
            }
        })

        rangee.addView(act.paragraphe(
            (if (signalees.contains(ent.id)) "⚠  " else "") + ent.fr).apply {
                setTextAppearance(R.style.Texte_Titre)
                setTextColor(act.couleurTheme(
                    if (signalees.contains(ent.id)) com.google.android.material.R.attr.colorPrimary
                    else com.google.android.material.R.attr.colorOnSurface))
                minHeight = act.dp(48)
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                setOnClickListener { FicheEntite.afficher(act, d, ent, null, null) }
            },
            LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        rangee.addView(act.paragraphe("✕").apply {
            setTextColor(act.couleurTheme(com.google.android.material.R.attr.colorOnSurfaceVariant))
            minWidth = act.dp(48); minHeight = act.dp(48)
            gravity = android.view.Gravity.CENTER
            isClickable = true
            contentDescription = "Écarter " + ent.fr
            setOnClickListener { ecarter(ent) }
        })
        return rangee
    }

    /** écarte à la main, et le dit — avec de quoi revenir tout de suite */
    private fun ecarter(ent: Entite) {
        e.ecarteesAlaMain.add(ent.id)
        rafraichir()
        com.google.android.material.snackbar.Snackbar
            .make(racine, ent.fr + " écartée", 6000)
            .setAction("Annuler") { e.ecarteesAlaMain.remove(ent.id); rafraichir() }
            .show()
    }

    /**
     * Les entités écartées à la main restent accessibles. On se trompe, et on s'en rend
     * compte trois minutes plus tard, pas pendant les six secondes du message.
     */
    private fun construireReprise() {
        zoneReprise.removeAllViews()
        val reprises = d.entites.filter { e.ecarteesAlaMain.contains(it.id) }
        zoneReprise.visibility = if (reprises.isEmpty()) View.GONE else View.VISIBLE
        if (reprises.isEmpty()) return

        zoneReprise.addView(act.paragraphe(
            "Écartées à la main — touchez pour les remettre", pale = true, petit = true))
        val rangee = com.google.android.material.chip.ChipGroup(act)
        val inflater = LayoutInflater.from(act)
        for (ent in reprises) {
            val c = inflater.inflate(R.layout.chip_etat, rangee, false) as Chip
            c.text = ent.fr
            c.habiller(Triple3.ECARTE)
            c.setOnClickListener {
                e.ecarteesAlaMain.remove(ent.id)
                rafraichir()
            }
            rangee.addView(c)
        }
        zoneReprise.addView(rangee)
    }

    private fun rafraichir() {
        for (i in 0 until chipsPreuves.childCount) {
            val c = chipsPreuves.getChildAt(i) as Chip
            c.habiller(
                when (e.preuves[c.tag as String]) {
                    1 -> Triple3.RETENU
                    2 -> Triple3.ECARTE
                    else -> Triple3.NEUTRE
                }
            )
        }
        for (i in 0 until chipsVitesses.childCount) {
            val c = chipsVitesses.getChildAt(i) as Chip
            c.habiller(
                when (e.pastilles[c.tag as String]) {
                    1 -> Triple3.RETENU
                    2 -> Triple3.ECARTE
                    else -> Triple3.NEUTRE
                }
            )
        }

        noteCache.visibility = if (e.resteCache) View.VISIBLE else View.GONE
        if (e.resteCache) noteCache.text =
            "L'entité n'a repéré personne : toute vitesse qui demande de vous voir, " +
                "de vous détecter ou de vous entendre est écartée du calcul."

        val restantes = m.restantes(e)
        val signalees = m.signalees(e)
        txtRestantes.text = when (restantes.size) {
            0 -> "Aucune entité ne colle — une observation est à revoir"
            1 -> "Une seule entité possible"
            else -> "${restantes.size} entités restantes"
        }

        // Deux formes pour deux gestes. Tant qu'il reste du monde on PARCOURT, donc des
        // pastilles en ligne ; quand il n'en reste que quelques-unes on DÉSIGNE, donc des
        // lignes où chaque action a sa propre cible.
        val peu = restantes.size <= 6
        defilRestantes.visibility = if (peu) View.GONE else View.VISIBLE
        lignesRestantes.visibility = if (peu) View.VISIBLE else View.GONE
        chipsRestantes.removeAllViews()
        lignesRestantes.removeAllViews()
        val inflater = LayoutInflater.from(act)

        if (peu) for (ent in restantes) lignesRestantes.addView(ligneEntite(ent, signalees))
        else for (ent in restantes) {
            val c = inflater.inflate(R.layout.chip_entite, chipsRestantes, false) as Chip
            c.text = (if (e.choisie == ent.id) "✓ " else "") +
                (if (signalees.contains(ent.id)) "⚠ " else "") + ent.fr
            if (signalees.contains(ent.id)) c.setTextColor(act.couleur(R.color.jp_signalee))
            // taper le nom OUVRE LA FICHE, rien d'autre : consulter n'est pas choisir
            c.setOnClickListener { FicheEntite.afficher(act, d, ent, null, null) }
            c.setOnCloseIconClickListener { ecarter(ent) }
            chipsRestantes.addView(c)
        }

        construireReprise()

        act.etatModifie()
    }
}
