package io.github.tibs971.journalphasmo

/**
 * Le moteur d'identification — portage fidèle de `reason()` et `evidenceReason()` de la
 * page web. L'ordre des règles est volontairement identique à celui du JavaScript : il
 * décide quelle raison est affichée quand plusieurs s'appliquent.
 *
 * Ce fichier n'importe rien d'Android : il se teste sur la machine de développement.
 * Voir `MoteurTest`, qui rejoue 1 300 états relevés sur la page web et exige les mêmes
 * listes d'entités.
 */
class Moteur(private val d: Donnees) {

    /** paliers de vitesse de base atteignables — réduits si le joueur est resté caché */
    fun paliers(e: Entite, s: Etat): List<String> {
        val cache = d.palierCache[e.id]
        if (s.resteCache && cache != null) return cache
        return d.palierParEntite[e.id] ?: listOf("normal")
    }

    /** plage de vitesse [min, max] — réduite si le joueur est resté caché */
    fun plage(e: Entite, s: Etat): List<Double> {
        val cache = d.vitesseCache[e.id]
        if (s.resteCache && cache != null) return cache
        return e.spd
    }

    /** la raison tirée des seules preuves, ou null si l'entité tient toujours */
    fun raisonPreuves(e: Entite, s: Etat): String? {
        val trouvees = d.ordrePreuves.filter { s.preuves[it] == 1 }
        val exclues = d.ordrePreuves.filter { s.preuves[it] == 2 }
        val siennes = e.ev

        // Le Mimic montre de faux orbes : les exclure l'élimine, les trouver ne prouve rien
        if (e.fake != null && exclues.contains(e.fake)) return "Orbes exclues"
        val vraiesTrouvees = trouvees.filter { !(e.fake != null && it == e.fake) }

        for (k in vraiesTrouvees) if (!siennes.contains(k)) return d.preuves[k]!!.fr + " trouvée"
        if (vraiesTrouvees.size > s.nbPreuves) return "trop de preuves pour ce mode"

        val restantes = siennes.filter { !exclues.contains(it) }
        if (restantes.size < s.nbPreuves) return "preuves exclues"

        if (e.forced != null && s.nbPreuves > 0 && s.nbPreuves < 3) {
            if (exclues.contains(e.forced)) return d.preuves[e.forced]!!.fr + " (forcée) exclue"
            // sa preuve forcée occupe forcément une des cases du mode : les autres preuves
            // trouvées ne peuvent donc pas toutes être différentes d'elle
            if (vraiesTrouvees.count { it != e.forced } > s.nbPreuves - 1)
                return d.preuves[e.forced]!!.fr + " (forcée) absente"
        }

        if (s.nbPreuves == 0 && vraiesTrouvees.isNotEmpty()) return "mode 0 preuve"
        return null
    }

    /** la raison pour laquelle cette entité est écartée, ou null si elle tient toujours */
    fun raison(e: Entite, s: Etat): String? {
        raisonPreuves(e, s)?.let { return it }

        val sante = s.santeMoyenne
        if (!s.chasseMaudite && sante != null && e.seuilMax < 999 && sante > e.seuilMax + 0.5)
            return "a chassé au-dessus de ${entier(e.seuilMax)} %"

        // Le Mimic copie n'importe quel comportement : les règles de vitesse ne l'écartent jamais
        val mimic = e.id == "mimic"

        if (!mimic) {
            if (s.ligneDeVue == "prog" && e.los == "non") return "sa vitesse ne monte jamais progressivement"
            if (s.ligneDeVue == "jump" && !d.accelereDunCoup.contains(e.id)) return "ne change jamais de vitesse d'un coup"
        }
        if (s.ligneDeVue == "non" && e.los == "oui") return "accélère toujours en ligne de vue"

        val gardes = s.pastilles.keys.filter { s.pastilles[it] == 1 }
        if (gardes.isNotEmpty() && gardes.none { paliers(e, s).contains(it) })
            return "vitesse de base " + gardes.joinToString(" / ") { d.paliers[it]!!.lowercase() } + " impossible"

        if (!mimic) for (c in s.pastilles.keys)
            if (s.pastilles[c] == 2 && paliers(e, s).contains(c))
                return "peut être " + d.paliers[c]!!.lowercase() + " (vitesse barrée)"

        val mesuree = s.vitesse
        if (s.vitesseMesuree && mesuree != null && !mimic) {
            val p = plage(e, s)
            if (mesuree < p[0] - 0.12 || mesuree > p[1] + 0.12)
                return "vitesse ${virgule(mesuree)} m/s hors plage"
        }

        // ce qui élimine, puis ce qui ne garde que certaines : deux passes, comme dans la page
        for (o in d.observations) {
            if (o.info != null) continue
            if (o.n3 && s.nbPreuves != 3) continue
            if (o.trig && s.chasseMaudite) continue
            val x = o.elimine(s.resteCache) ?: continue
            if (o.id != null && s.cochees.contains(o.id) && x.contains(e.id))
                return "exclu : " + o.t!!.lowercase()
        }
        if (!mimic) for (o in d.observations) {
            if (o.info != null) continue
            if (o.n3 && s.nbPreuves != 3) continue
            if (o.trig && s.chasseMaudite) continue
            val g = o.garde(s.resteCache) ?: continue
            if (o.id != null && s.cochees.contains(o.id) && !g.contains(e.id))
                return "ne colle pas : " + o.t!!.lowercase()
        }

        if (s.ecarteesAlaMain.contains(e.id)) return "éliminée à la main"
        return null
    }

    /** les entités qui tiennent encore, dans l'ordre du journal */
    fun restantes(s: Etat): List<Entite> = d.entites.filter { raison(it, s) == null }

    /** celles qu'une observation « signale sans éliminer » désigne */
    fun signalees(s: Etat): Set<String> {
        val out = LinkedHashSet<String>()
        for (o in d.observations) {
            val f = o.flag ?: continue
            if (o.id != null && s.cochees.contains(o.id)) out.addAll(f)
        }
        return out
    }

    private fun entier(x: Double): String =
        if (x == Math.floor(x)) x.toLong().toString() else virgule(x)

    /** 1.7 -> « 1,70 », comme la page web */
    private fun virgule(x: Double): String {
        val n = Math.round(x * 100.0)
        return "${n / 100}," + (n % 100).toString().padStart(2, '0')
    }
}
