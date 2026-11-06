/* teq.js — equivalence stricte avant/apres refactor.
   Balaye des milliers d'etats et compare les listes d'entites survivantes.
   Compare par le TEXTE des observations : un identifiant a ete renomme volontairement. */
const {chromium}=require('playwright'); const fs=require('fs');
const crypto=require('crypto');
/* usage : node teq.js <ancien.html> <nouveau.html>  (defaut : AVANT-refactor.html standalone.html) */
const FICHIERS=[process.argv[2]||'AVANT-refactor.html', process.argv[3]||'standalone.html'];

const BALAYAGE = () => {
  const out={};
  const raz=()=>{ S.obs={};S.cat={};S.ev={};S.manual={};S.los=null;S.hid=false;
                  S.spdf=false;S.speed=null;S.san=null;S.n=3; appliquerCache(); };
  const vivants=()=>GHOSTS.filter(g=>!reason(g)).map(g=>g.id).sort().join(',');

  // 1 — chaque observation seule, en mode normal puis « reste cache »
  for(const o of OBS){
    if(!o.id||o.info) continue;
    for(const hid of [false,true]){
      raz(); S.hid=hid; S.obs[o.id]=1; update();
      out['obs|'+o.t+'|'+hid] = vivants();
    }
  }
  // 2 — les 128 combinaisons de preuves, a 3 preuves puis a 2
  for(let m=0;m<128;m++){
    for(const n of [2,3]){
      raz(); S.n=n;
      EV_ORDER.forEach((e,i)=>{ if(m>>i&1) S.ev[e]=1; });
      update(); out['ev|'+m+'|'+n] = vivants();
    }
  }
  // 3 — les 243 etats des pastilles de vitesse (neutre / garder / barrer)
  const cles=Object.keys(CAT);
  for(let m=0;m<243;m++){
    raz(); let v=m;
    for(const k of cles){ const t=v%3; v=(v-t)/3; if(t) S.cat[k]=t; }
    update(); out['cat|'+m] = vivants();
  }
  // 4 — idem avec « reste cache », la ou le bug precedent se cachait
  for(let m=0;m<243;m++){
    raz(); S.hid=true; let v=m;
    for(const k of cles){ const t=v%3; v=(v-t)/3; if(t) S.cat[k]=t; }
    update(); out['cathid|'+m] = vivants();
  }
  // 5 — vitesse mesuree au Tap, par pas de 0,05 m/s, avec et sans cache
  for(let x=90;x<=320;x+=5){
    for(const hid of [false,true]){
      raz(); S.hid=hid; S.spdf=true; S.speed=x/100; update();
      out['spd|'+x+'|'+hid] = vivants();
    }
  }
  // 6 — ligne de vue × sante moyenne
  for(const los of [null,'jump','prog','non'])
    for(const san of [null,0,10,25,40,50,60,75,100]){
      raz(); S.los=los; S.san=san; update();
      out['los|'+los+'|'+san] = vivants();
    }
  // 7 — fiches completes : tout le contenu affiche de chaque entite
  for(const g of GHOSTS)
    out['fiche|'+g.id] = JSON.stringify([g.fr,g.en,g.ev,g.seuil,g.seuilMax,g.vit,g.spd,g.los,
                                         g.key,g.capa,g.indices,g.confirm,g.elim,g.forced,g.fake]);
  // 8 — les tables de reference, telles qu'elles seront lues par l'app Android
  out['tables'] = JSON.stringify([EV,EV_ORDER,EQUIP,DIFFS,SIZES,MAPS,CURSED,MYTHS,
                                  CAT,GCAT,HIDCAT,HIDSPD,DEPCOND,HUNTSET,JUMP,LOSLBL,BLINK,KEYLBL]);
  return out;
};

(async()=>{
  const b=await chromium.launch();
  const lu={};
  for(const f of FICHIERS){
    const p=await b.newPage();
    const err=[]; p.on('pageerror',e=>err.push(e.message));
    await p.setContent(fs.readFileSync(f,'utf8'));
    await p.waitForTimeout(1000);
    lu[f]=await p.evaluate(BALAYAGE);
    console.log(f.padEnd(22)+Object.keys(lu[f]).length+' etats'+(err.length?'  ERREURS: '+err.join(' | '):''));
    await p.close();
  }
  const [a,n]=[lu[FICHIERS[0]],lu[FICHIERS[1]]];
  const ca=Object.keys(a), cn=Object.keys(n);
  const manquants=ca.filter(k=>!(k in n)), nouveaux=cn.filter(k=>!(k in a));
  const diff=ca.filter(k=>k in n && a[k]!==n[k]);
  console.log('\netats absents du nouveau : '+(manquants.length||'0'));
  manquants.slice(0,10).forEach(k=>console.log('   '+k));
  console.log('etats nouveaux           : '+(nouveaux.length||'0'));
  nouveaux.slice(0,10).forEach(k=>console.log('   '+k));
  console.log('etats DIVERGENTS         : '+(diff.length||'0'));
  diff.slice(0,20).forEach(k=>console.log('   '+k+'\n      avant : '+a[k]+'\n      apres : '+n[k]));
  const h=o=>crypto.createHash('sha256').update(JSON.stringify(Object.keys(o).sort().map(k=>[k,o[k]]))).digest('hex').slice(0,16);
  console.log('\nempreinte avant : '+h(a)+'\nempreinte apres : '+h(n));
  console.log(diff.length||manquants.length||nouveaux.length ? '\n>>> DIVERGENCE' : '\n>>> IDENTIQUE');
  await b.close();
})();
