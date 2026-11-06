package io.github.tibs971.journalphasmo

/**
 * Le contenu du journal, lu depuis donnees.json — le même fichier que la page web et
 * l'application Windows. Une correction faite là vaut pour les trois.
 *
 * Rien n'est recopié ici : si une fiche change, ce fichier n'a pas à être touché.
 */

/** Une entité. */
class Entite(
    val id: String,
    val fr: String,
    val en: String,
    val ev: List<String>,
    val seuil: String,
    val seuilMax: Double,
    val vit: String,
    val spd: List<Double>,
    val los: String,
    val key: String,
    val capa: List<String>,
    val indices: List<String>,
    val confirm: List<String>,
    val elim: List<String>,
    /** preuve toujours présente chez elle, si elle en a une */
    val forced: String?,
    /** preuve qu'elle fait croire sans l'avoir — seul Le Mimic, avec les orbes */
    val fake: String?
)

/**
 * Une ligne de l'enquête : une case à cocher, un en-tête de groupe ou un encart.
 *
 * Les variantes `gAll`/`gHid` portent les deux versions d'une même case : la normale et
 * celle du mode « resté caché ». C'est ce qui remplace la mutation du tableau que fait
 * la page web dans `appliquerCache()` — ici rien n'est modifié, on demande la bonne
 * version au moment de s'en servir.
 */
class Observation(
    val id: String?,
    val t: String?,
    val grp: String?,
    val info: String?,
    val s: String?,
    val sAll: String?,
    val sHid: String?,
    val g: List<String>?,
    val gAll: List<String>?,
    val gHid: List<String>?,
    val x: List<String>?,
    val xAll: List<String>?,
    val xHid: List<String>?,
    /** signale sans éliminer */
    val flag: List<String>?,
    val txt: String?,
    /** ne vaut qu'en mode 3 preuves */
    val n3: Boolean,
    /** sans objet pendant une chasse maudite */
    val trig: Boolean
) {
    fun garde(cache: Boolean): List<String>? = if (gAll != null) (if (cache) gHid else gAll) else g
    fun elimine(cache: Boolean): List<String>? = if (xAll != null) (if (cache) xHid else xAll) else x
    fun soustitre(cache: Boolean): String? = if (sAll != null) (if (cache) sHid else sAll) else s
    val estLigne: Boolean get() = id != null && info == null
}

/** Une preuve. */
class Preuve(val fr: String, val long: String, val touche: String)

/** Un départ de vitesse qui dépend de l'état de la partie. */
class DepartConditionnel(val condition: String, val geste: String)

// Les conversions depuis le JSON lu sont forcément non vérifiées : le format du fichier
// est garanti par construire.js côté projet, et par les tests côté application.
@Suppress("UNCHECKED_CAST")
class Donnees(brut: Map<String, Any?>) {

    val entites: List<Entite>
    val preuves: Map<String, Preuve>
    val ordrePreuves: List<String>
    val paliers: Map<String, String>
    val palierParEntite: Map<String, List<String>>
    val palierCache: Map<String, List<String>>
    val vitesseCache: Map<String, List<Double>>
    val departConditionnel: Map<String, DepartConditionnel>
    val departSur: Map<String, List<String>>
    val departCache: Map<String, String>
    val departToujoursConditionnel: List<String>
    val accelereDunCoup: List<String>
    val libellesLigneDeVue: Map<String, String>

    /** les lignes du fichier, PLUS celles que le journal calcule lui-même */
    val observations: List<Observation>

    /** tables conservées telles quelles pour les écrans qui les afficheront */
    val equipement: List<Map<String, Any?>>
    val difficultes: List<Map<String, Any?>>
    val taillesCarte: List<Map<String, Any?>>
    val cartes: List<Map<String, Any?>>
    val objetsMaudits: List<Map<String, Any?>>
    val ideesRecues: List<List<String>>
    val dureesChasse: Map<String, List<Double>>
    val clignotement: List<Map<String, Any?>>
    val libellesRaccourcis: Map<String, String>

    init {
        fun obj(c: String): Map<String, Any?> = brut[c] as? Map<String, Any?>
            ?: throw IllegalArgumentException("donnees.json : la table « $c » manque ou n'est pas un objet")

        fun tab(c: String): List<Any?> =
            brut[c] as? List<Any?>
                ?: throw IllegalArgumentException("donnees.json : la table « $c » manque ou n'est pas un tableau")

        fun mapDe(v: Any?): Map<String, Any?> = v as Map<String, Any?>
        fun textes(v: Any?): List<String>? = (v as? List<*>)?.map { it as String }
        fun nombres(v: Any?): List<Double> = (v as? List<*>)?.map { it as Double } ?: emptyList()

        entites = tab("GHOSTS").map { e ->
            val m = mapDe(e)
            Entite(
                id = m["id"] as String, fr = m["fr"] as String, en = m["en"] as String,
                ev = textes(m["ev"]) ?: emptyList(),
                seuil = m["seuil"] as String, seuilMax = m["seuilMax"] as Double,
                vit = m["vit"] as String, spd = nombres(m["spd"]),
                los = m["los"] as String, key = m["key"] as String,
                capa = textes(m["capa"]) ?: emptyList(),
                indices = textes(m["indices"]) ?: emptyList(),
                confirm = textes(m["confirm"]) ?: emptyList(),
                elim = textes(m["elim"]) ?: emptyList(),
                forced = m["forced"] as? String, fake = m["fake"] as? String
            )
        }

        preuves = obj("EV").mapValues { (_, v) ->
            val m = mapDe(v)
            Preuve(m["fr"] as String, m["long"] as String, m["key"] as String)
        }
        ordrePreuves = tab("EV_ORDER").map { it as String }
        paliers = obj("CAT").mapValues { (_, v) -> v as String }
        palierParEntite = obj("GCAT").mapValues { (_, v) -> textes(v) ?: emptyList() }
        palierCache = obj("HIDCAT").mapValues { (_, v) -> textes(v) ?: emptyList() }
        vitesseCache = obj("HIDSPD").mapValues { (_, v) -> nombres(v) }
        departConditionnel = obj("DEPCOND").mapValues { (_, v) ->
            val m = mapDe(v); DepartConditionnel(m["c"] as String, m["a"] as String)
        }
        departSur = obj("DEPSUR").mapValues { (_, v) -> textes(v) ?: emptyList() }
        departCache = obj("HIDSTART").mapValues { (_, v) -> v as String }
        departToujoursConditionnel = tab("HIDCOND").map { it as String }
        accelereDunCoup = tab("JUMP").map { it as String }
        libellesLigneDeVue = obj("LOSLBL").mapValues { (_, v) -> v as String }

        equipement = tab("EQUIP").map { mapDe(it) }
        difficultes = tab("DIFFS").map { mapDe(it) }
        taillesCarte = tab("SIZES").map { mapDe(it) }
        cartes = tab("MAPS").map { mapDe(it) }
        objetsMaudits = tab("CURSED").map { mapDe(it) }
        ideesRecues = tab("MYTHS").map { textes(it) ?: emptyList() }
        dureesChasse = obj("HUNTSET").mapValues { (_, v) -> nombres(v) }
        clignotement = tab("BLINK").map { mapDe(it) }
        libellesRaccourcis = obj("KEYLBL").mapValues { (_, v) -> v as String }

        val statiques = tab("OBS").map { o ->
            val m = mapDe(o)
            Observation(
                id = m["id"] as? String, t = m["t"] as? String,
                grp = m["grp"] as? String, info = m["info"] as? String,
                s = m["s"] as? String, sAll = m["sAll"] as? String, sHid = m["sHid"] as? String,
                g = textes(m["g"]), gAll = textes(m["gAll"]), gHid = textes(m["gHid"]),
                x = textes(m["x"]), xAll = textes(m["xAll"]), xHid = textes(m["xHid"]),
                flag = textes(m["flag"]), txt = m["txt"] as? String,
                n3 = m["n3"] == true, trig = m["trig"] != null
            )
        }
        observations = statiques + preuvesForcees() + vitessesDeDepart()
    }

    private fun nom(id: String): String = entites.firstOrNull { it.id == id }?.fr ?: id

    private fun ligne(
        id: String, t: String, s: String? = null,
        g: List<String>? = null, x: List<String>? = null, flag: List<String>? = null,
        gAll: List<String>? = null, gHid: List<String>? = null,
        sAll: String? = null, sHid: String? = null, txt: String? = null
    ) = Observation(id, t, null, null, s, sAll, sHid, g, gAll, gHid, x, null, null, flag, txt, false, false)

    private fun entete(titre: String) =
        Observation(null, null, titre, null, null, null, null, null, null, null, null, null, null, null, null, false, false)

    private fun encart(texte: String) =
        Observation(null, null, null, texte, null, null, null, null, null, null, null, null, null, null, null, false, false)

    /**
     * Preuves forcées — portage fidèle du générateur de la page web.
     * Généré depuis les fiches pour rester juste si les preuves d'une entité changent.
     */
    private fun preuvesForcees(): List<Observation> {
        val par = LinkedHashMap<String, MutableList<String>>()
        entites.filter { it.forced != null }.forEach { par.getOrPut(it.forced!!) { ArrayList() }.add(it.id) }
        val cles = ordrePreuves.filter { par.containsKey(it) }
        if (cles.isEmpty()) return emptyList()
        val out = ArrayList<Observation>()
        out.add(entete("Preuves forcées — à appliquer vous-même"))
        out.add(encart("Une preuve forcée est toujours affichée : en 1 ou 2 preuves le journal l'applique déjà tout seul dès qu'une preuve est trouvée ou exclue. Ces cases servent à trancher vous-même — en 3 preuves, ou avant d'avoir terminé vos relevés. À vous de juger si vous les retenez."))
        for (k in cles) {
            val ids = par[k]!!.toList()
            val nomPreuve = preuves[k]!!.fr
            out.add(ligne("fx_keep_$k", "Preuve forcée retenue : $nomPreuve",
                s = "Vous pariez que l'entité a $nomPreuve en preuve forcée", g = ids))
            out.add(ligne("fx_out_$k", "Preuve forcée écartée : $nomPreuve",
                s = "Vous pariez que l'entité n'a pas $nomPreuve en preuve forcée", x = ids))
        }
        return out
    }

    /**
     * Vitesse de départ — portage fidèle du second générateur.
     * À ne pas confondre avec les paliers : ici c'est la vitesse à laquelle l'entité PART
     * dès qu'elle se manifeste.
     */
    private fun vitessesDeDepart(): List<Observation> {
        fun peut(id: String, c: String) = (palierParEntite[id] ?: listOf("normal")).contains(c)
        val fixes = entites.filter { !palierParEntite.containsKey(it.id) }.map { it.id }

        val out = ArrayList<Observation>()
        out.add(entete("Vitesse de départ — à quelle vitesse elle PART"))
        out.add(encart("Les pastilles en haut de l'enquête disent quelles vitesses l'entité PEUT atteindre au cours du contrat. Ces cases-ci disent à quelle vitesse elle EST PARTIE dès qu'elle s'est manifestée — mesurez dans les toutes premières secondes, avant toute accélération. Les entités dont le départ dépend de la partie restent dans chaque liste où elles peuvent tomber — elles ne sont jamais éliminées à tort."))

        for (c in listOf("tlent", "lent", "normal", "rapide", "trapide")) {
            val sur = (departSur[c] ?: emptyList()) + (if (c == "normal") fixes else emptyList())
            val cond = departConditionnel.keys.filter { peut(it, c) }
            val ids = sur + cond
            if (ids.isEmpty()) continue

            val nSur = if (sur.size > 8) "toutes les entités à vitesse constante" else sur.joinToString(", ") { nom(it) }
            val sub = (if (sur.isNotEmpty()) "Départ sûr : $nSur" else "Aucun départ sûr à cette vitesse") +
                (if (cond.isNotEmpty()) " · ${cond.size}" + (if (cond.size > 1) " autres dépendent" else " autre dépend") + " de la partie" else "")

            // version « resté caché » : les conditions qui dépendent du joueur ne se déclenchent pas
            val surH = sur + departCache.keys.filter { departCache[it] == c && !sur.contains(it) }
            val condH = departToujoursConditionnel.filter { peut(it, c) }
            val idsH = surH + condH
            val subH = (if (surH.size > 8) "Départ sûr : toutes les entités à vitesse constante"
                        else "Départ sûr : " + surH.joinToString(", ") { nom(it) }) +
                (if (condH.isNotEmpty()) " · ${condH.size}" + (if (condH.size > 1) " dépendent" else " dépend") + " encore de la partie" else "")

            out.add(ligne("dep_$c", "Démarre à : ${paliers[c]}", s = sub,
                g = ids, gAll = ids, gHid = idsH, sAll = sub, sHid = subH))
        }

        out.add(ligne("samespeed", "Même vitesse de base sur 3 chasses ou plus",
            s = "N'élimine personne, et c'est voulu : une entité à vitesse variable répète la même valeur si la condition n'a pas bougé. Marque celles qui AURAIENT PU changer — ce sont les moins probables",
            flag = palierParEntite.keys.filter { it != "mimic" }, txt = "Signale sans éliminer"))
        out.add(encart("Départs non fixes : " +
            departConditionnel.keys.joinToString(" · ") { "${nom(it)} (${departConditionnel[it]!!.condition})" } +
            ". Le Mimic copie l'entité qu'il imite. Une fois une case cochée, le détail et le geste qui lève le doute s'affichent au-dessus des entités restantes."))
        return out
    }

    companion object {
        fun depuis(texte: String): Donnees {
            val m = Json.lire(texte) as? Map<String, Any?>
                ?: throw IllegalArgumentException("donnees.json : la racine doit être un objet")
            return Donnees(m)
        }
    }
}
