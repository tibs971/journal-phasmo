package io.github.tibs971.journalphasmo

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.R as MatR
import com.google.android.material.chip.Chip

/** Les trois états d'une puce de l'enquête. */
enum class Triple3 { NEUTRE, RETENU, ECARTE }

/**
 * Lit une couleur dans le thème courant plutôt que de l'écrire en dur : c'est la règle
 * de la doc Android (« Avoid using hardcoded colors... Use theme attributes or
 * night-qualified resources instead »), et c'est ce qui fait que le mode sombre suit
 * le réglage du téléphone sans une ligne de code en plus.
 */
fun Context.couleurTheme(attr: Int): Int {
    val t = android.util.TypedValue()
    theme.resolveAttribute(attr, t, true)
    return if (t.resourceId != 0) resources.getColor(t.resourceId, theme) else t.data
}

fun Context.couleur(id: Int): Int = resources.getColor(id, theme)

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

/**
 * Habille une puce selon son état. Le geste est le même que sur la page web et dans
 * l'application Windows : un appui retient, deux appuis écartent, trois reviennent au
 * neutre. Apprendre une fois suffit pour les trois supports.
 *
 * La zone tactile fait 48 dp même quand la puce n'en mesure que 32 : c'est le défaut de
 * `chipMinTouchTargetSize`, et c'est ce qui permet de taper sans regarder.
 */
fun Chip.habiller(etat: Triple3) {
    val ctx = context
    isCheckable = false
    isClickable = true
    chipStrokeWidth = ctx.dp(1).toFloat()
    isChipIconVisible = etat != Triple3.NEUTRE
    isCloseIconVisible = false

    when (etat) {
        Triple3.NEUTRE -> {
            chipBackgroundColor = ColorStateList.valueOf(ctx.couleurTheme(MatR.attr.colorSurface))
            chipStrokeColor = ColorStateList.valueOf(ctx.couleurTheme(MatR.attr.colorOutline))
            setTextColor(ctx.couleurTheme(MatR.attr.colorOnSurface))
            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        Triple3.RETENU -> {
            val fond = ctx.couleur(R.color.jp_trouvee)
            val sur = ctx.couleur(R.color.jp_sur_trouvee)
            chipBackgroundColor = ColorStateList.valueOf(fond)
            chipStrokeColor = ColorStateList.valueOf(fond)
            setTextColor(sur)
            setChipIconResource(R.drawable.ic_coche)
            chipIconTint = ColorStateList.valueOf(sur)
            paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        Triple3.ECARTE -> {
            val fond = ctx.couleur(R.color.jp_exclue)
            val sur = ctx.couleur(R.color.jp_sur_exclue)
            chipBackgroundColor = ColorStateList.valueOf(fond)
            chipStrokeColor = ColorStateList.valueOf(ctx.couleurTheme(MatR.attr.colorOutline))
            setTextColor(sur)
            setChipIconResource(R.drawable.ic_croix)
            chipIconTint = ColorStateList.valueOf(sur)
            paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }
}

/**
 * Un titre de section. En casse normale, volontairement : les libellés tout en
 * majuscules au-dessus de chaque bloc sont le réflexe le plus courant et le plus
 * anonyme, et ils se lisent moins vite — ce qui compte ici plus qu'ailleurs.
 */
fun Context.etiquette(texte: String): TextView = TextView(this).apply {
    text = texte
    setTextColor(couleurTheme(MatR.attr.colorPrimary))
    setTextAppearance(R.style.Texte_Etiquette)
    setPadding(0, dp(18), 0, dp(6))
}

/**
 * Une valeur mesurée : vitesse, durée, pourcentage. En largeur fixe, pour que deux
 * relevés posés l'un sous l'autre s'alignent et se comparent d'un coup d'oeil au lieu
 * de se lire. C'est le seul endroit où la mono a le droit d'apparaître.
 */
fun Context.releve(texte: String, grand: Boolean = false): TextView = TextView(this).apply {
    text = texte
    setTextAppearance(if (grand) R.style.Releve_Grand else R.style.Releve)
    setTextColor(couleurTheme(MatR.attr.colorOnSurface))
}

/**
 * Un filet d'un pixel. Sépare sans encadrer : c'est ce qui remplace les cartes.
 * Il tire sur `colorOutlineVariant`, plus doux que le contour des boutons : un
 * séparateur n'a pas à crier aussi fort que le bord d'un contrôle.
 */
fun Context.filet(marge: Int = 0): android.view.View = android.view.View(this).apply {
    setBackgroundColor(couleurTheme(MatR.attr.colorOutlineVariant))
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)
    ).apply { topMargin = marge; bottomMargin = marge }
}

/** Barre le texte — une entité écartée est rayée, comme sur un carnet. */
fun TextView.barrer(oui: Boolean) {
    paintFlags = if (oui) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
}

/** Une ligne de texte courant. */
fun Context.paragraphe(texte: String, pale: Boolean = false, petit: Boolean = false): TextView =
    TextView(this).apply {
        text = texte
        setTextAppearance(if (petit) R.style.Texte_Petit else R.style.Texte_Corps)
        setTextColor(couleurTheme(if (pale) MatR.attr.colorOnSurfaceVariant else MatR.attr.colorOnSurface))
        setPadding(0, 0, 0, dp(4))
    }

fun Context.colonne(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

/**
 * Compare sans tenir compte des accents ni de la casse : « temperature » doit trouver
 * « température », et « deogen » doit trouver « Deogen ». Les fiches sont en français,
 * et personne ne tape les accents dans une recherche en pleine partie.
 */
fun String.plat(): String =
    java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** vrai si le texte contient la recherche, accents et casse ignorés */
fun contient(texte: String?, recherche: String): Boolean =
    recherche.isBlank() || (texte ?: "").plat().contains(recherche.plat())

/**
 * Branche une barre de recherche déjà présente dans la mise en page.
 * Le filtrage se fait à la frappe : aucune validation à presser.
 */
fun brancherRecherche(racine: android.view.View, indice: String, action: (String) -> Unit) {
    val bloc = racine.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.blocRecherche)
    val champ = racine.findViewById<android.widget.EditText>(R.id.champRecherche)
    if (bloc == null || champ == null) return
    bloc.hint = indice
    champ.addTextChangedListener(object : android.text.TextWatcher {
        override fun afterTextChanged(s: android.text.Editable?) = action(s?.toString() ?: "")
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
    })
}
