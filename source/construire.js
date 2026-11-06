/* construire.js — fusionne modele.html + donnees.json en standalone.html.
   Les donnees du journal vivent dans donnees.json : source unique partagee
   avec l'application Android. standalone.html est GENERE par ce script. */
const fs=require('fs');
const sortieNom = process.argv[2] || 'standalone.html';   // ex. : node construire.js ../phasmo.html
const modele=fs.readFileSync(__dirname+'/modele.html','utf8');
const brut=fs.readFileSync(__dirname+'/donnees.json','utf8');

let d; try{ d=JSON.parse(brut); }
catch(e){ console.error('donnees.json invalide : '+e.message); process.exit(1); }

// garde-fous : ce que le journal exige pour fonctionner
const EXIGE=["EV","EV_ORDER","GHOSTS","EQUIP","DIFFS","SIZES","MAPS","CURSED","MYTHS","OBS",
             "CAT","GCAT","HIDCAT","HIDSPD","DEPCOND","HUNTSET","JUMP","LOSLBL","BLINK","KEYLBL","DEPSUR","HIDSTART","HIDCOND"];
const manque=EXIGE.filter(n=>!(n in d));
if(manque.length){ console.error('tables absentes de donnees.json : '+manque.join(', ')); process.exit(1); }
const sansId=d.GHOSTS.filter(g=>!g.id||!g.fr).map(g=>g.fr||g.id||'?');
if(sansId.length){ console.error('entites sans id ou sans nom : '+sansId.join(', ')); process.exit(1); }
const vus=new Set(), doubles=[];
for(const g of d.GHOSTS){ if(vus.has(g.id)) doubles.push(g.id); vus.add(g.id); }
for(const o of d.OBS){ if(!o.id) continue; if(vus.has('obs:'+o.id)) doubles.push(o.id); vus.add('obs:'+o.id); }
if(doubles.length){ console.error('identifiants en double : '+doubles.join(', ')); process.exit(1); }
// toute entite citee par une observation doit exister
const ids=new Set(d.GHOSTS.map(g=>g.id)); const fantomes=new Set();
// sAll/sHid sont des sous-titres (chaines) : seuls les tableaux listent des entites
for(const o of d.OBS) for(const c of ['g','x','flag','gAll','gHid','xAll','xHid'])
  if(Array.isArray(o[c])) for(const id of o[c]) if(!ids.has(id)) fantomes.add(o.id+' -> '+id);
for(const k of Object.keys(d.GCAT).concat(Object.keys(d.HIDCAT),Object.keys(d.DEPCOND),Object.keys(d.HIDSTART)))
  if(!ids.has(k)) fantomes.add('table -> '+k);
for(const c of Object.keys(d.DEPSUR)) for(const id of d.DEPSUR[c])
  if(!ids.has(id)) fantomes.add('DEPSUR.'+c+' -> '+id);
for(const id of d.HIDCOND) if(!ids.has(id)) fantomes.add('HIDCOND -> '+id);
for(const c of Object.values(d.HIDSTART)) if(!(c in d.CAT)) fantomes.add('HIDSTART -> palier inconnu '+c);
for(const c of Object.keys(d.DEPSUR)) if(!(c in d.CAT)) fantomes.add('DEPSUR -> palier inconnu '+c);
if(fantomes.size){ console.error('entites inconnues citees : '+[...fantomes].join(', ')); process.exit(1); }

// JSON.stringify compact, puis neutralisation de « < » : aucun </script possible
const charge=JSON.stringify(d).replace(/</g,'\\u003c');
if(!modele.includes('/*@DONNEES@*/{}')){ console.error('marqueur /*@DONNEES@*/{} absent de modele.html'); process.exit(1); }
const sortie=modele.replace('/*@DONNEES@*/{}', charge);

fs.writeFileSync(sortieNom,sortie);
console.log(sortieNom+' genere : '+fs.statSync(sortieNom).size+' octets');
console.log('  '+d.GHOSTS.length+' entites · '+d.OBS.filter(o=>o.id&&!o.info).length+' observations · '
            +d.EQUIP.length+' equipements · '+d.MAPS.length+' cartes · '+d.MYTHS.length+' idees recues');
