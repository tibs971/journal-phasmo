package io.github.tibs971.journalphasmo

/**
 * Lecteur JSON minimal, en Kotlin pur.
 *
 * Pourquoi ne pas utiliser une bibliothèque : `org.json` d'Android est remplacé par des
 * méthodes vides dans les tests unitaires, et `kotlinx.serialization` ajoute un plugin et
 * une version de plus à faire concorder. Le fichier lu est le nôtre et son format est
 * stable, donc un lecteur de cent lignes suffit — et il se comporte à l'identique sur la
 * machine de développement et sur le téléphone.
 *
 * Les nombres sont tous rendus en Double : le JSON ne distingue pas les entiers.
 */
object Json {

    fun lire(texte: String): Any? {
        val l = Lecteur(texte)
        val v = l.valeur()
        l.blancs()
        if (!l.fini()) l.erreur("du texte suit la valeur")
        return v
    }

    private class Lecteur(val s: String) {
        var i = 0

        fun fini() = i >= s.length

        fun erreur(message: String): Nothing =
            throw IllegalArgumentException("JSON invalide, position $i : $message")

        fun blancs() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\t' || s[i] == '\n' || s[i] == '\r')) i++
        }

        fun valeur(): Any? {
            blancs()
            if (fini()) erreur("une valeur était attendue")
            return when (s[i]) {
                '{' -> objet()
                '[' -> tableau()
                '"' -> chaine()
                't' -> mot("true", true)
                'f' -> mot("false", false)
                'n' -> mot("null", null)
                else -> nombre()
            }
        }

        fun mot(attendu: String, valeur: Any?): Any? {
            if (!s.startsWith(attendu, i)) erreur("« $attendu » était attendu")
            i += attendu.length
            return valeur
        }

        // LinkedHashMap : l'ordre des clés du fichier est conservé, et le moteur en dépend
        fun objet(): Map<String, Any?> {
            i++ // {
            val m = LinkedHashMap<String, Any?>()
            blancs()
            if (!fini() && s[i] == '}') { i++; return m }
            while (true) {
                blancs()
                if (fini() || s[i] != '"') erreur("un nom de champ était attendu")
                val cle = chaine()
                blancs()
                if (fini() || s[i] != ':') erreur("« : » était attendu après le champ « $cle »")
                i++
                m[cle] = valeur()
                blancs()
                if (fini()) erreur("objet non refermé")
                when (s[i]) {
                    ',' -> i++
                    '}' -> { i++; return m }
                    else -> erreur("« , » ou « } » était attendu")
                }
            }
        }

        fun tableau(): List<Any?> {
            i++ // [
            val l = ArrayList<Any?>()
            blancs()
            if (!fini() && s[i] == ']') { i++; return l }
            while (true) {
                l.add(valeur())
                blancs()
                if (fini()) erreur("tableau non refermé")
                when (s[i]) {
                    ',' -> i++
                    ']' -> { i++; return l }
                    else -> erreur("« , » ou « ] » était attendu")
                }
            }
        }

        fun chaine(): String {
            i++ // "
            val b = StringBuilder()
            while (true) {
                if (fini()) erreur("chaîne non refermée")
                val c = s[i]
                when {
                    c == '"' -> { i++; return b.toString() }
                    c == '\\' -> {
                        i++
                        if (fini()) erreur("échappement incomplet")
                        when (val e = s[i]) {
                            '"' -> b.append('"')
                            '\\' -> b.append('\\')
                            '/' -> b.append('/')
                            'b' -> b.append('\b')
                            'f' -> b.append('\u000C')
                            'n' -> b.append('\n')
                            'r' -> b.append('\r')
                            't' -> b.append('\t')
                            'u' -> {
                                if (i + 4 >= s.length) erreur("échappement \\u incomplet")
                                val hex = s.substring(i + 1, i + 5)
                                val code = hex.toIntOrNull(16) ?: erreur("« \\u$hex » n'est pas valide")
                                b.append(code.toChar())
                                i += 4
                            }
                            else -> erreur("échappement « \\$e » inconnu")
                        }
                        i++
                    }
                    else -> { b.append(c); i++ }
                }
            }
        }

        fun nombre(): Double {
            val debut = i
            if (!fini() && (s[i] == '-' || s[i] == '+')) i++
            while (!fini() && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' || s[i] == 'E' ||
                        ((s[i] == '-' || s[i] == '+') && (s[i - 1] == 'e' || s[i - 1] == 'E')))) i++
            val brut = s.substring(debut, i)
            return brut.toDoubleOrNull() ?: erreur("« $brut » n'est pas un nombre")
        }
    }
}
