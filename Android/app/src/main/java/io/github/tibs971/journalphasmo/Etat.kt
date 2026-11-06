package io.github.tibs971.journalphasmo

/**
 * Ce que le joueur a coché. Rien de plus : les règles sont dans [Moteur], le contenu
 * dans [Donnees]. C'est l'équivalent de l'objet `S` de la page web, réduit à ce dont
 * l'identification a besoin.
 */
class Etat {
    /** nombre de preuves de la difficulté : 0, 1, 2 ou 3 */
    var nbPreuves: Int = 3

    /** santé moyenne de l'équipe au moment de la chasse, en pourcentage ; null si non relevée */
    var santeMoyenne: Double? = null

    /** comportement observé en ligne de vue : "jump", "prog", "non", ou null */
    var ligneDeVue: String? = null

    /** le joueur est resté caché et n'a jamais été détecté */
    var resteCache: Boolean = false

    /** une vitesse a été mesurée au Tap */
    var vitesseMesuree: Boolean = false
    var vitesse: Double? = null

    /** la chasse en cours est une chasse maudite (seuil de santé sans objet) */
    var chasseMaudite: Boolean = false

    /** une chasse maudite a déjà eu lieu sur ce contrat */
    var chasseMauditePassee: Boolean = false

    /** par preuve : 0 inconnue, 1 trouvée, 2 exclue */
    val preuves: MutableMap<String, Int> = LinkedHashMap()

    /** par palier de vitesse : 1 garder seulement celles-là, 2 les barrer */
    val pastilles: MutableMap<String, Int> = LinkedHashMap()

    /** identifiants des cases de l'enquête cochées */
    val cochees: MutableSet<String> = LinkedHashSet()

    /** entités écartées à la main par le joueur */
    val ecarteesAlaMain: MutableSet<String> = LinkedHashSet()

    /**
     * L'entité que le joueur désigne comme étant la bonne. Volontairement séparée du
     * fait d'ouvrir sa fiche : consulter n'est pas choisir.
     */
    var choisie: String? = null

    fun raz() {
        nbPreuves = 3; santeMoyenne = null; ligneDeVue = null
        resteCache = false; vitesseMesuree = false; vitesse = null
        chasseMaudite = false; chasseMauditePassee = false
        preuves.clear(); pastilles.clear(); cochees.clear(); ecarteesAlaMain.clear()
        choisie = null
    }
}
