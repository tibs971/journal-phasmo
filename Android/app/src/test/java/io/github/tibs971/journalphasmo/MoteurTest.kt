package io.github.tibs971.journalphasmo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Preuve que le moteur Kotlin se comporte exactement comme celui de la page web.
 *
 * `reference-moteur.json` a été produit en exécutant la page web et en relevant, pour
 * 1 300 états différents, la liste des entités qui restaient. Ce test rejoue chacun de
 * ces états et exige la même liste. Une seule différence fait échouer la compilation
 * de la version de test — c'est voulu : une divergence ici veut dire qu'une entité
 * serait mal identifiée dans le jeu.
 *
 * Lancer : ./gradlew test        (aucun téléphone ni émulateur nécessaire)
 */
class MoteurTest {

    private fun ressource(nom: String): String =
        javaClass.classLoader!!.getResourceAsStream(nom)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalStateException(
                "$nom est absent de app/src/test/resources — recopie-le depuis le dossier source du projet Windows"
            )

    private val donnees: Donnees by lazy { Donnees.depuis(ressource("donnees.json")) }
    private val moteur: Moteur by lazy { Moteur(donnees) }

    @Suppress("UNCHECKED_CAST")
    private fun reference(): List<Map<String, Any?>> {
        val racine = Json.lire(ressource("reference-moteur.json")) as Map<String, Any?>
        return racine["cas"] as List<Map<String, Any?>>
    }

    private fun etatDepuis(m: Map<String, Any?>): Etat {
        val e = Etat()
        e.nbPreuves = (m["n"] as Double).toInt()
        e.santeMoyenne = m["san"] as Double?
        e.ligneDeVue = m["los"] as String?
        e.resteCache = m["hid"] == true
        e.vitesseMesuree = m["spdf"] == true
        e.vitesse = m["speed"] as Double?
        e.chasseMaudite = m["chm"] == true
        e.chasseMauditePassee = m["chmDone"] == true

        @Suppress("UNCHECKED_CAST") val cat = m["cat"] as Map<String, Any?>
        for ((k, v) in cat) e.pastilles[k] = (v as Double).toInt()

        @Suppress("UNCHECKED_CAST") val ev = m["ev"] as Map<String, Any?>
        for ((k, v) in ev) e.preuves[k] = (v as Double).toInt()

        @Suppress("UNCHECKED_CAST") val obs = m["obs"] as Map<String, Any?>
        for ((k, v) in obs) if ((v as Double).toInt() != 0) e.cochees.add(k)

        @Suppress("UNCHECKED_CAST") val man = m["manual"] as Map<String, Any?>
        for ((k, v) in man) if ((v as Double).toInt() != 0) e.ecarteesAlaMain.add(k)
        return e
    }

    @Test
    fun `le moteur rend exactement les memes entites que la page web`() {
        val cas = reference()
        assertTrue("la référence est vide : le fichier est tronqué", cas.size > 1000)

        val ecarts = ArrayList<String>()
        for (c in cas) {
            @Suppress("UNCHECKED_CAST")
            val attendu = (c["restantes"] as List<String>).sorted()
            @Suppress("UNCHECKED_CAST")
            val etat = etatDepuis(c["etat"] as Map<String, Any?>)
            val obtenu = moteur.restantes(etat).map { it.id }.sorted()
            if (obtenu != attendu) {
                val enTrop = obtenu - attendu.toSet()
                val manquantes = attendu - obtenu.toSet()
                ecarts.add(
                    "cas « ${c["nom"]} » : " +
                        (if (manquantes.isNotEmpty()) "écartées à tort ${manquantes}  " else "") +
                        (if (enTrop.isNotEmpty()) "gardées à tort ${enTrop}" else "")
                )
            }
        }
        if (ecarts.isNotEmpty()) {
            val apercu = ecarts.take(15).joinToString("\n  ")
            throw AssertionError(
                "${ecarts.size} cas sur ${cas.size} divergent de la page web :\n  $apercu" +
                    (if (ecarts.size > 15) "\n  … et ${ecarts.size - 15} autres" else "")
            )
        }
    }

    @Test
    fun `les donnees sont completes et coherentes`() {
        assertEquals("nombre d'entités", 30, donnees.entites.size)
        assertEquals("identifiants d'entités en double",
            donnees.entites.size, donnees.entites.map { it.id }.toSet().size)

        val ids = donnees.entites.map { it.id }.toSet()
        val lignes = donnees.observations.filter { it.estLigne }
        assertEquals("identifiants d'observations en double",
            lignes.size, lignes.mapNotNull { it.id }.toSet().size)

        for (o in donnees.observations) {
            for (liste in listOfNotNull(o.g, o.gAll, o.gHid, o.x, o.xAll, o.xHid, o.flag))
                for (id in liste)
                    assertTrue("l'observation « ${o.id} » cite une entité inconnue : $id", ids.contains(id))
        }
        for (e in donnees.entites) {
            assertTrue("${e.fr} : plage de vitesse mal formée", e.spd.size == 2 && e.spd[0] <= e.spd[1])
            assertTrue("${e.fr} : fiche incomplète", e.capa.isNotEmpty() && e.indices.isNotEmpty())
        }
    }

    @Test
    fun `les observations calculees sont bien generees`() {
        val calculees = donnees.observations.mapNotNull { it.id }
            .filter { it.startsWith("fx_") || it.startsWith("dep_") || it == "samespeed" }
        assertTrue("aucune observation calculée : les générateurs n'ont pas tourné", calculees.size >= 11)
        assertTrue("les cases de preuve forcée manquent", calculees.any { it.startsWith("fx_keep_") })
        assertTrue("les cases de vitesse de départ manquent", calculees.any { it.startsWith("dep_") })
    }

    @Test
    fun `le mode reste cache reduit bien les vitesses`() {
        val e = Etat()
        val revenant = donnees.entites.first { it.id == "revenant" }
        e.resteCache = false
        assertEquals(listOf("tlent", "trapide"), moteur.paliers(revenant, e))
        e.resteCache = true
        assertEquals(listOf("tlent"), moteur.paliers(revenant, e))
        assertEquals(listOf(1.0, 1.0), moteur.plage(revenant, e))
    }
}
