/* vers-android.js — recopie la source unique vers le projet Android.
   Usage : node vers-android.js <chemin du projet JournalPhasmoAndroid>
   Exemple : node vers-android.js ../../JournalPhasmoAndroid

   A lancer apres chaque correction de donnees.json, en meme temps que
   « node construire.js ../phasmo.html » pour la page. */
const fs=require('fs'), path=require('path');
const cible=process.argv[2];
if(!cible){ console.error('indique le dossier du projet Android'); process.exit(1); }

const dest=[
  ['donnees.json',        path.join(cible,'app/src/main/assets/donnees.json')],
  ['donnees.json',        path.join(cible,'app/src/test/resources/donnees.json')],
  ['reference-moteur.json', path.join(cible,'app/src/test/resources/reference-moteur.json')]
];
let fait=0, sautes=[];
for(const [src,dst] of dest){
  const s=path.join(__dirname,src);
  if(!fs.existsSync(s)){ sautes.push(src+' (absent — lance exporter.js pour la reference)'); continue; }
  fs.mkdirSync(path.dirname(dst),{recursive:true});
  fs.copyFileSync(s,dst); fait++;
  console.log('  -> '+path.relative(cible,dst)+'  ('+fs.statSync(dst).size+' octets)');
}
console.log(fait+' fichier(s) recopie(s)'+(sautes.length?' · sautes : '+sautes.join(', '):''));
if(sautes.length) process.exit(1);
console.log('\nPense a relancer les tests du projet Android :  gradlew test');
