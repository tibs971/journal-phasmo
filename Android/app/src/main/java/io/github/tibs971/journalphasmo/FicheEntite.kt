package io.github.tibs971.journalphasmo

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * La fiche complète d'une entité, telle qu'elle est écrite dans donnees.json.
 * Ouverte depuis la liste des entités comme depuis le bandeau de l'enquête.
 */
object FicheEntite {

    fun afficher(
        ctx: Context,
        d: Donnees,
        e: Entite,
        raison: String?,
        choisir: (() -> Unit)? = null
    ) {
        val corps = ctx.colonne().apply { setPadding(ctx.dp(20), ctx.dp(8), ctx.dp(20), ctx.dp(8)) }

        if (raison != null) corps.addView(ctx.paragraphe("Écartée : $raison", pale = true))

        corps.addView(ctx.paragraphe("${e.vit}  ·  chasse sous ${e.seuil}", pale = true))
        corps.addView(
            ctx.paragraphe("Preuves : " + e.ev.joinToString(", ") { d.preuves[it]?.fr ?: it })
        )
        if (e.forced != null) corps.addView(
            ctx.paragraphe(
                "Preuve toujours présente : " + (d.preuves[e.forced]?.fr ?: e.forced),
                pale = true
            )
        )
        if (e.key.isNotBlank()) corps.addView(ctx.paragraphe(depouiller(e.key)))

        bloc(ctx, corps, "Ce qu'elle fait", e.capa)
        bloc(ctx, corps, "Comment la repérer", e.indices)
        bloc(ctx, corps, "Ce qui la confirme", e.confirm)
        bloc(ctx, corps, "Ce qui l'élimine", e.elim)

        val boite = MaterialAlertDialogBuilder(ctx)
            .setTitle(e.fr)
            .setView(ScrollView(ctx).apply { addView(corps) })
            .setNegativeButton("Fermer", null)
        // consulter n'est pas choisir : l'action de designer l'entite est explicite
        if (choisir != null) boite.setPositiveButton("C'est celle-là") { _, _ -> choisir() }
        boite.show()
    }

    private fun bloc(ctx: Context, parent: LinearLayout, titre: String, lignes: List<String>) {
        if (lignes.isEmpty()) return
        parent.addView(ctx.etiquette(titre))
        for (l in lignes) parent.addView(ctx.paragraphe("·  " + depouiller(l)))
    }

    /** les fiches portent un peu de mise en forme HTML, héritée de la page web */
    fun depouiller(t: String): String =
        t.replace(Regex("<[^>]+>"), "").replace("&nbsp;", " ").trim()
}
