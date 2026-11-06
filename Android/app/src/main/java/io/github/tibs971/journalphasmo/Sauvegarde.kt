package io.github.tibs971.journalphasmo

import android.content.Context

/**
 * Garde l'enquête en cours. Deux raisons, pas une :
 *  - tourner le téléphone recrée l'écran, et on ne veut pas perdre ce qui est coché ;
 *  - reprendre une partie après avoir fermé l'application doit retrouver l'enquête.
 *
 * Volontairement sans bibliothèque de sérialisation : des clés simples suffisent.
 */
object Sauvegarde {

    private const val FICHIER = "journal-phasmo"

    fun lire(ctx: Context, e: Etat) {
        val p = ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
        if (!p.contains("nbPreuves")) return
        e.nbPreuves = p.getInt("nbPreuves", 3)
        e.santeMoyenne = p.getFloat("sante", -1f).let { if (it < 0f) null else it.toDouble() }
        e.ligneDeVue = p.getString("ldv", "")!!.ifEmpty { null }
        e.resteCache = p.getBoolean("cache", false)
        e.vitesseMesuree = p.getBoolean("vitesseMesuree", false)
        e.vitesse = p.getFloat("vitesse", -1f).let { if (it < 0f) null else it.toDouble() }
        e.chasseMaudite = p.getBoolean("maudite", false)
        e.chasseMauditePassee = p.getBoolean("mauditePassee", false)

        e.preuves.clear(); e.pastilles.clear(); e.cochees.clear(); e.ecarteesAlaMain.clear()
        for (ligne in p.getStringSet("preuves", emptySet())!!) paire(ligne)?.let { e.preuves[it.first] = it.second }
        for (ligne in p.getStringSet("pastilles", emptySet())!!) paire(ligne)?.let { e.pastilles[it.first] = it.second }
        e.cochees.addAll(p.getStringSet("cochees", emptySet())!!)
        e.ecarteesAlaMain.addAll(p.getStringSet("ecartees", emptySet())!!)
        e.choisie = p.getString("choisie", "")!!.ifEmpty { null }
    }

    fun ecrire(ctx: Context, e: Etat) {
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit().apply {
            putInt("nbPreuves", e.nbPreuves)
            putFloat("sante", e.santeMoyenne?.toFloat() ?: -1f)
            putString("ldv", e.ligneDeVue ?: "")
            putBoolean("cache", e.resteCache)
            putBoolean("vitesseMesuree", e.vitesseMesuree)
            putFloat("vitesse", e.vitesse?.toFloat() ?: -1f)
            putBoolean("maudite", e.chasseMaudite)
            putBoolean("mauditePassee", e.chasseMauditePassee)
            putStringSet("preuves", e.preuves.map { "${it.key}=${it.value}" }.toSet())
            putStringSet("pastilles", e.pastilles.map { "${it.key}=${it.value}" }.toSet())
            putStringSet("cochees", e.cochees.toSet())
            putStringSet("ecartees", e.ecarteesAlaMain.toSet())
            putString("choisie", e.choisie ?: "")
            apply()
        }
    }

    /**
     * Repart à zéro — mais seulement l'ENQUÊTE. Le thème choisi et les noms donnés aux
     * minuteurs sont des réglages, pas des relevés : les effacer aussi serait une
     * mauvaise surprise au milieu d'une partie.
     */
    fun effacer(ctx: Context, e: Etat) {
        e.raz()
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit().apply {
            for (c in listOf("nbPreuves", "sante", "ldv", "cache", "vitesseMesuree",
                             "vitesse", "maudite", "mauditePassee", "preuves", "pastilles",
                             "cochees", "ecartees", "choisie")) remove(c)
            apply()
        }
    }

    // --- thème choisi à la main ---

    const val THEME_AUTO = "auto"

    fun theme(ctx: Context): String =
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .getString("theme", THEME_AUTO) ?: THEME_AUTO

    fun definirTheme(ctx: Context, cle: String) {
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .edit().putString("theme", cle).apply()
    }

    // --- réglages des minuteurs : nom choisi et affichage en secondes ---

    fun nomMinuteur(ctx: Context, cle: String, defaut: String): String =
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .getString("minuteur.nom.$cle", null) ?: defaut

    fun renommerMinuteur(ctx: Context, cle: String, nom: String) {
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .edit().putString("minuteur.nom.$cle", nom).apply()
    }

    fun minuteurEnSecondes(ctx: Context, cle: String): Boolean =
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .getBoolean("minuteur.sec.$cle", false)

    fun basculerUnite(ctx: Context, cle: String, secondes: Boolean) {
        ctx.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .edit().putBoolean("minuteur.sec.$cle", secondes).apply()
    }

    private fun paire(ligne: String): Pair<String, Int>? {
        val i = ligne.lastIndexOf('=')
        if (i <= 0) return null
        val v = ligne.substring(i + 1).toIntOrNull() ?: return null
        return ligne.substring(0, i) to v
    }
}
