/* exporter.js — fige le comportement du moteur JavaScript dans un fichier de reference.
   Le test Kotlin de l'application Android rejoue ces cas et doit rendre exactement
   les memes listes d'entites. C'est la preuve que le portage est fidele. */
const {chromium}=require('playwright'); const fs=require('fs');
/* usage : node exporter.js [page.html] [sortie.json]
   defaut : la page generee a cote, vers reference-moteur.json */
const PAGE   = process.argv[2] || 'standalone.html';
const SORTIE = process.argv[3] || 'reference-moteur.json';
(async()=>{
  const b=await chromium.launch(); const p=await b.newPage();
  const err=[]; p.on('pageerror',e=>err.push(e.message));
  await p.setContent(fs.readFileSync(PAGE,'utf8'));
  await p.waitForTimeout(1000);
  const ref=await p.evaluate(()=>{
    const cas=[];
    const neutre=()=>({n:3,san:null,los:null,hid:false,spdf:false,speed:null,
                       chm:false,chmDone:false,cat:{},obs:{},manual:{},ev:{}});
    const poser=e=>{ S.n=e.n;S.san=e.san;S.los=e.los;S.hid=e.hid;S.spdf=e.spdf;S.speed=e.speed;
                     S.chm=e.chm;S.chmDone=e.chmDone;
                     S.cat={...e.cat}; S.obs={...e.obs}; S.manual={...e.manual};
                     S.ev={}; EV_ORDER.forEach(k=>S.ev[k]=e.ev[k]||0);
                     appliquerCache(); };
    const pousser=(nom,e)=>{ poser(e); cas.push({nom,etat:e,
                              restantes:GHOSTS.filter(g=>!reason(g)).map(g=>g.id).sort()}); };

    // 1 — chaque observation seule, en normal puis « reste cache »
    for(const o of OBS){ if(!o.id||o.info) continue;
      for(const hid of [false,true]){ const e=neutre(); e.hid=hid; e.obs[o.id]=1;
        pousser('obs/'+o.id+(hid?'/cache':''),e); } }
    // 2 — les 128 combinaisons de preuves, en 2 puis 3 preuves
    for(let m=0;m<128;m++) for(const n of [2,3]){
      const e=neutre(); e.n=n; EV_ORDER.forEach((k,i)=>{ if(m>>i&1) e.ev[k]=1; });
      pousser('ev/'+m+'/'+n,e); }
    // 2b — preuves exclues (etat 2), pour couvrir l'autre branche
    for(let m=0;m<128;m++){ const e=neutre(); EV_ORDER.forEach((k,i)=>{ if(m>>i&1) e.ev[k]=2; });
      pousser('evx/'+m,e); }
    // 3 — les 243 etats des pastilles, sans puis avec « reste cache »
    const cles=Object.keys(CAT);
    for(const hid of [false,true]) for(let m=0;m<243;m++){
      const e=neutre(); e.hid=hid; let v=m;
      for(const k of cles){ const t=v%3; v=(v-t)/3; if(t) e.cat[k]=t; }
      pousser('cat/'+m+(hid?'/cache':''),e); }
    // 4 — vitesse mesuree au Tap
    for(const hid of [false,true]) for(let x=90;x<=320;x+=5){
      const e=neutre(); e.hid=hid; e.spdf=true; e.speed=x/100;
      pousser('spd/'+x+(hid?'/cache':''),e); }
    // 5 — ligne de vue x sante moyenne x chasse maudite
    for(const los of [null,'jump','prog','non']) for(const san of [null,0,10,25,40,50,60,75,100])
      for(const chm of [false,true]){
        const e=neutre(); e.los=los; e.san=san; e.chm=chm;
        pousser('los/'+los+'/'+san+'/'+chm,e); }
    // 6 — elimination a la main
    for(const g of GHOSTS){ const e=neutre(); e.manual[g.id]=1; pousser('main/'+g.id,e); }
    // 7 — quelques combinaisons croisees, la ou les regles se melangent
    const croise=[
      {n:3,ev:{emf:1,frz:1},cat:{normal:2},los:'jump'},
      {n:2,ev:{sb:1},hid:true,spdf:true,speed:1.7},
      {n:3,obs:{dep_tlent:1},hid:true},
      {n:3,obs:{samespeed:1},cat:{rapide:1}},
      {n:1,ev:{dots:1},san:20},
      {n:3,obs:{slowburst:1},cat:{tlent:1}},
      {n:3,obs:{fx_keep_orb:1},ev:{orb:2}},
      {n:0,ev:{emf:1}},
      {n:3,san:60,chm:true,los:'non'},
      {n:2,ev:{uv:1,wr:2},cat:{lent:2,trapide:2},hid:true}
    ];
    croise.forEach((c,i)=>{ const e=Object.assign(neutre(),c); pousser('croise/'+i,e); });
    return cas;
  });
  if(err.length){ console.error('erreurs de page : '+err.join(' | ')); process.exit(1); }
  const sortie={ genere:new Date().toISOString().slice(0,10),
                 source:'moteur JavaScript du Journal Phasmo (reason/evidenceReason)',
                 cas:ref };
  fs.writeFileSync(SORTIE, JSON.stringify(sortie,null,1));
  console.log(SORTIE+' : '+ref.length+' cas · '+fs.statSync(SORTIE).size+' octets');
  const vides=ref.filter(c=>c.restantes.length===0).length;
  const pleins=ref.filter(c=>c.restantes.length===30).length;
  console.log('  cas sans aucune entite restante : '+vides+' · cas avec les 30 : '+pleins);
  await b.close();
})();
