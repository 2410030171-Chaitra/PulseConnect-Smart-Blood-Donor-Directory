/*
  RBCBackground (React 18 UMD)
  - Premium, realistic red blood cells rendered on Canvas
  - Lightweight: requestAnimationFrame + gradients, no 3D libs
  - Accessible: slows on hover, respects prefers-reduced-motion
  - Responsive: scales with container; fewer cells on small screens
  - Customizable via CSS variables:
      --rbc-count, --rbc-speed, --rbc-vessel-bg,
      --rbc-body, --rbc-rim-dark, --rbc-rim-bright
*/
(function(){
  if (!window.React || !window.ReactDOM) return; // bail if React isn't available

  const { useRef, useEffect } = React;

  function RBCBackground(props){
    const canvasRef = useRef(null);
    const rafRef = useRef(0);

    useEffect(()=>{
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      const DPR = Math.max(1, window.devicePixelRatio || 1);
      const prefersReduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

      // Sizing to CSS pixels and DPI
      function fit(){
        const cssW = canvas.clientWidth || 380;
        const cssH = canvas.clientHeight || 380;
        canvas.width = Math.floor(cssW * DPR);
        canvas.height = Math.floor(cssH * DPR);
        ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
      }
      fit();

      // Colors and config from CSS variables
      function readVars(){
        const root = getComputedStyle(document.documentElement);
        const crimson = (root.getPropertyValue('--rbc-body').trim() || root.getPropertyValue('--primary-color').trim() || '#dc143c');
        const rimDark = (root.getPropertyValue('--rbc-rim-dark').trim() || root.getPropertyValue('--primary-dark').trim() || '#8b0e16');
        const rimBright = (root.getPropertyValue('--rbc-rim-bright').trim() || root.getPropertyValue('--primary-light').trim() || '#ff4d6d');
        const countVar = parseInt(root.getPropertyValue('--rbc-count'));
        const speedVar = parseFloat(root.getPropertyValue('--rbc-speed'));
        const vb = (root.getPropertyValue('--rbc-vessel-bg').trim() || '0');
        const vesselBg = vb === '1' || vb.toLowerCase() === 'true';
        const mode = (root.getPropertyValue('--rbc-mode').trim() || 'drops').toLowerCase();
        // extras for filling empty space uniquely
  const plasmaVar = (root.getPropertyValue('--rbc-plasma').trim() || '1');
        const plasma = plasmaVar === '1' || plasmaVar.toLowerCase() === 'true';
        const plateletsVar = (root.getPropertyValue('--rbc-platelets').trim() || '1');
        const platelets = plateletsVar === '1' || plateletsVar.toLowerCase() === 'true';
  const dropsVar = (root.getPropertyValue('--rbc-drops').trim() || '1');
  const dropsEnabled = dropsVar === '1' || dropsVar.toLowerCase() === 'true';
        const hueVar = parseFloat(root.getPropertyValue('--rbc-plasma-hue'));
        const plasmaHue = Number.isFinite(hueVar) ? hueVar : 350; // deep red-magenta
        const intensityVar = parseFloat(root.getPropertyValue('--rbc-plasma-intensity'));
        const plasmaIntensity = Number.isFinite(intensityVar) ? intensityVar : 0.65;
        const count = isNaN(countVar) ? ((canvas.clientWidth||380) < 640 ? 10 : 18) : countVar;
        const speed = isNaN(speedVar) ? 1 : Math.max(0.1, speedVar);
        return { crimson, rimDark, rimBright, count, speed, vesselBg, mode, plasma, platelets, plasmaHue, plasmaIntensity, dropsEnabled };
      }
      let cfg = readVars();

      // Helpers
      const W = () => canvas.clientWidth || 380;
      const H = () => canvas.clientHeight || 380;

  // RBC shading (biconcave disc)
      function drawRBC(rx, ry){
        // base body
        const base = ctx.createRadialGradient(0, 0, Math.min(rx,ry)*0.25, 0, 0, Math.max(rx,ry));
        base.addColorStop(0.00, cfg.crimson);
        base.addColorStop(0.70, cfg.crimson);
        base.addColorStop(1.00, cfg.rimDark);
        ctx.fillStyle = base;
        ctx.beginPath(); ctx.ellipse(0,0,rx,ry,0,0,Math.PI*2); ctx.fill();
        // central pallor
        ctx.save(); ctx.globalCompositeOperation = 'screen';
        const pallor = ctx.createRadialGradient(0, 0, 0, 0, 0, Math.min(rx,ry)*0.55);
        pallor.addColorStop(0.00, 'rgba(255,235,235,0.55)');
        pallor.addColorStop(1.00, 'rgba(255,235,235,0)');
        ctx.fillStyle = pallor; ctx.beginPath();
        ctx.ellipse(0,0,rx*0.7,ry*0.7,0,0,Math.PI*2); ctx.fill(); ctx.restore();
        // rim darkening
        ctx.save(); ctx.globalCompositeOperation = 'multiply';
        const rimShade = ctx.createRadialGradient(0, 0, Math.min(rx,ry)*0.6, 0, 0, Math.max(rx,ry));
        rimShade.addColorStop(0.00, 'rgba(0,0,0,0)');
        rimShade.addColorStop(1.00, 'rgba(0,0,0,0.35)');
        ctx.fillStyle = rimShade; ctx.beginPath();
        ctx.ellipse(0,0,rx,ry,0,0,Math.PI*2); ctx.fill(); ctx.restore();
        // subtle edge highlight
        ctx.save(); ctx.globalCompositeOperation = 'screen';
        const hl = ctx.createLinearGradient(-rx,-ry,rx,ry);
        hl.addColorStop(0.50, 'rgba(255,255,255,0)');
        hl.addColorStop(0.58, 'rgba(255,255,255,0.22)');
        hl.addColorStop(0.66, 'rgba(255,255,255,0)');
        ctx.strokeStyle = hl; ctx.lineWidth = Math.max(1, Math.min(rx,ry)*0.06);
        ctx.beginPath(); ctx.ellipse(0,0,rx*0.96,ry*0.96,0,0,Math.PI*2); ctx.stroke(); ctx.restore();
      }

      // Teardrop geometry (thin, realistic) and shading
      function drawDropPath(r){
        ctx.beginPath();
        ctx.moveTo(0, -r);
        // Right edge
        ctx.quadraticCurveTo(r*0.38, -r*0.85, r*0.45, -r*0.20);
        ctx.quadraticCurveTo(r*0.80,  r*0.65, 0, r*0.90);
        // Left edge
        ctx.quadraticCurveTo(-r*0.80, r*0.65, -r*0.45, -r*0.20);
        ctx.quadraticCurveTo(-r*0.38, -r*0.85, 0, -r);
        ctx.closePath();
      }
      function fillDrop(r){
        // Deep body gradient
        const body = ctx.createRadialGradient(-r*0.15, -r*0.15, r*0.05, 0, 0, r*1.05);
        body.addColorStop(0.00, cfg.crimson);
        body.addColorStop(0.55, cfg.crimson);
        body.addColorStop(0.85, cfg.rimDark);
        body.addColorStop(1.00, '#3b0508');
        ctx.fillStyle = body;
        ctx.fill();

        // Specular highlight streak (screen)
        ctx.save();
        ctx.globalCompositeOperation = 'screen';
        const hl = ctx.createLinearGradient(-r*0.5, -r*0.8, -r*0.05, -r*0.05);
        hl.addColorStop(0.0, 'rgba(255,255,255,0.0)');
        hl.addColorStop(0.5, 'rgba(255,255,255,0.85)');
        hl.addColorStop(1.0, 'rgba(255,255,255,0.0)');
        ctx.fillStyle = hl;
        ctx.beginPath();
        ctx.moveTo(-r*0.45, -r*0.75);
        ctx.quadraticCurveTo(-r*0.25, -r*0.45, -r*0.15, -r*0.12);
        ctx.quadraticCurveTo(-r*0.12, r*0.25, -r*0.38, r*0.65);
        ctx.quadraticCurveTo(-r*0.55, r*0.20, -r*0.50, -r*0.25);
        ctx.closePath();
        ctx.fill();
        ctx.restore();

        // Inner darkening (multiply)
        ctx.save();
        ctx.globalCompositeOperation = 'multiply';
        const core = ctx.createRadialGradient(0, r*0.25, r*0.05, 0, 0, r*0.95);
        core.addColorStop(0.0, 'rgba(0,0,0,0.18)');
        core.addColorStop(1.0, 'rgba(0,0,0,0.0)');
        ctx.fillStyle = core;
        drawDropPath(r);
        ctx.fill();
        ctx.restore();

        // Subtle rim line
        ctx.strokeStyle = 'rgba(255,255,255,0.18)';
        ctx.lineWidth = Math.max(1, r*0.035);
        ctx.stroke();
      }

      // Optional vessel background (soft, premium look)
      function drawVesselBackground(){
        const w = canvas.width, h = canvas.height;
        const cx = w/2, cy = h/2; const r = Math.hypot(cx, cy);
        const g = ctx.createRadialGradient(cx, cy, r*0.06, cx, cy, r*1.05);
        g.addColorStop(0.00, 'rgba(255,235,235,0.95)');
        g.addColorStop(0.25, 'rgba(255,120,120,0.55)');
        g.addColorStop(1.00, 'rgba(90,0,30,0.95)');
        ctx.fillStyle = g; ctx.fillRect(0,0,w,h);
        // faint spiral ridges
        ctx.save(); ctx.translate(cx, cy); ctx.globalAlpha = 0.08;
        const maxR = r*0.92;
        for(let i=0;i<6;i++){
          const ang = (i/6)*Math.PI*2 + performance.now()/7000;
          ctx.rotate(ang);
          const ring = ctx.createRadialGradient(0,0, maxR*0.25, 0,0, maxR);
          ring.addColorStop(0,'rgba(255,255,255,0)');
          ring.addColorStop(1,'rgba(255,255,255,0.9)');
          ctx.strokeStyle = ring; ctx.lineWidth = Math.max(1, Math.min(w,h)*0.02);
          ctx.beginPath(); ctx.ellipse(0,0,maxR*0.88,maxR*0.62,0,0,Math.PI*2); ctx.stroke();
        }
        ctx.restore();
      }

      // Unique plasma ribbons (fills empty space elegantly)
      function drawPlasmaRibbons(time){
        if (!cfg.plasma) return;
        const w = canvas.width/DPR, h = canvas.height/DPR;
        const cx = w*0.7, spread = Math.min(w,h)*0.35;
        const baseAlpha = 0.26 * cfg.plasmaIntensity;
        const hue = cfg.plasmaHue; // HSL for soft vivid strokes
        ctx.save();
        ctx.globalCompositeOperation = 'screen';
        for (let k=0;k<3;k++){
          const t = time/1000 * (0.25 + k*0.05) + k*1.7;
          const amp = spread*(0.45 - k*0.08);
          const y0 = h*0.25 + k*h*0.18;
          const y1 = h*0.65 + k*h*0.12;
          const x0 = cx - amp*0.9, x3 = cx + amp*0.9;
          const cp1x = cx - amp*Math.sin(t*0.9), cp1y = y0 + amp*0.25*Math.cos(t*0.7);
          const cp2x = cx + amp*Math.cos(t*0.8), cp2y = y1 + amp*0.22*Math.sin(t*0.6);
          // glow underlay
          ctx.save();
          ctx.filter = 'blur(10px)';
          ctx.globalAlpha = baseAlpha*0.8;
          ctx.strokeStyle = `hsl(${hue} 85% 60% / 1)`;
          ctx.lineWidth = Math.max(8, Math.min(w,h)*0.020);
          ctx.beginPath();
          ctx.moveTo(x0,y0);
          ctx.bezierCurveTo(cp1x,cp1y, cp2x,cp2y, x3,y1);
          ctx.stroke();
          ctx.restore();
          // crisp ribbon
          ctx.globalAlpha = baseAlpha*1.2;
          const grad = ctx.createLinearGradient(x0,y0,x3,y1);
          grad.addColorStop(0, `hsl(${hue} 85% 55% / 0.9)`);
          grad.addColorStop(0.5, `hsl(${(hue+10)%360} 95% 70% / 0.7)`);
          grad.addColorStop(1, `hsl(${hue} 85% 55% / 0.9)`);
          ctx.strokeStyle = grad;
          ctx.lineWidth = Math.max(2.5, Math.min(w,h)*0.008);
          ctx.beginPath();
          ctx.moveTo(x0,y0);
          ctx.bezierCurveTo(cp1x,cp1y, cp2x,cp2y, x3,y1);
          ctx.stroke();
        }
        ctx.restore();
      }

      // Helical flow model (simple perspective with z-depth)
      function makeCell(depth){
        const rBase = Math.min(W(), H()) * 0.075;
        const r = rBase * (0.6 + depth*0.6);
        return {
          theta: Math.random()*Math.PI*2,
          z: (0.2 + Math.random()*0.8) * 1200,
          rCyl: (0.25 + Math.random()*0.6) * Math.min(W(),H())*0.38,
          omega: (0.3 + Math.random()*0.6) * (Math.random()<0.5?-1:1),
          vz: 60 * (0.7 + Math.random()*0.6),
          r,
          rot: Math.random()*Math.PI,
          t: Math.random()*Math.PI*2,
          wobble: 0.10 + Math.random()*0.15,
          aspect: 0.55 + Math.random()*0.15,
          depth: 1
        };
      }

      let cells = [];
      function resetCells(){
        cells = [];
        const n = cfg.count;
        for (let i=0;i<n;i++) cells.push(makeCell(0.4 + Math.random()*0.9));
      }
      resetCells();

      function step(d, dt){
        d.theta += d.omega * dt;
        d.z -= d.vz * dt;
        d.t += dt * 0.7;
        if (d.z < 60) Object.assign(d, makeCell(0.4 + Math.random()*0.9));
      }

      function draw(d){
        ctx.save();
        const w = canvas.width, h = canvas.height; const cx = w/2, cy = h/2;
        const FOV = 520; const scale = FOV / (FOV + d.z);
        const xw = d.rCyl * Math.cos(d.theta);
        const yw = d.rCyl * Math.sin(d.theta) * 0.75;
        const sx = cx + xw * scale; const sy = cy + yw * scale;
        ctx.translate(sx, sy);
        const wob = 1 + Math.sin(d.t + d.theta)*d.wobble*0.3;
        const rp = d.r * scale * 1.2; const rx = rp * wob; const ry = rp * (d.aspect*(2 - wob));
        ctx.rotate(d.rot + Math.sin(d.t*0.6)*0.25);
        // soft multiplied shadow (helps blend on purple)
        ctx.save(); ctx.globalCompositeOperation='multiply'; ctx.globalAlpha=0.20 * scale;
        ctx.filter = `blur(${(2 + (1-scale)*4).toFixed(2)}px)`;
        ctx.beginPath(); ctx.ellipse(rx*0.05, ry*0.55, rx*0.6, ry*0.25, 0, 0, Math.PI*2);
        ctx.fillStyle='rgba(0,0,0,1)'; ctx.fill(); ctx.restore();
        // DOF blur + alpha
        const blurPx = (1 - scale) * 2.2; ctx.filter = `blur(${blurPx.toFixed(2)}px)`; ctx.globalAlpha = 0.65 + 0.35*scale;
        drawRBC(rx, ry);
        ctx.filter = 'none'; ctx.globalAlpha = 1; ctx.restore();
      }

      // -- Drops mode --
      function makeDrop(depth){
        const rBase = Math.min(W(), H()) * 0.11; // visibly thin but prominent
        const r = rBase * (0.7 + depth*0.4);
        const cx = (W()*0.58) + (Math.random()-0.5)*W()*0.18;
        const cy = (H()*0.48) + (Math.random()-0.5)*H()*0.18;
        return {
          x: cx, y: cy, r,
          rot: Math.random()*Math.PI*2,
          t: Math.random()*Math.PI*2,
          wobble: 0.06 + Math.random()*0.08,
          depth,
          vx: (Math.random()*4-2) * depth,
          vy: (Math.random()*6-3) * depth
        };
      }
      let drops = [];
      // Platelets (tiny discs with amber hue)
      function makePlatelet(){
        const r = Math.min(W(),H()) * (0.008 + Math.random()*0.006);
        return {
          x: W()* (0.55 + Math.random()*0.35),
          y: H()* (0.20 + Math.random()*0.60),
          r,
          t: Math.random()*Math.PI*2,
          vx: (Math.random()*10+8) * (Math.random()<0.5? -1: 1) * 0.15,
          vy: (Math.random()*6-3) * 0.12,
          a: 0.45 + Math.random()*0.35
        };
      }
      let platelets = [];
      function resetDrops(){
        drops = [];
        if (cfg.dropsEnabled) {
          const n = Math.max(1, Math.min(6, cfg.count||3));
          for(let i=0;i<n;i++) drops.push(makeDrop(0.6 + Math.random()*0.6));
        }
        platelets = [];
        if (cfg.platelets){
          const m = 14; // subtle sprinkle
          for(let i=0;i<m;i++) platelets.push(makePlatelet());
        }
      }
      resetDrops();
      function stepDrop(d, dt){
        const flow = Math.sin((performance.now()/1500) + d.depth)*4 * d.depth;
        d.x += (d.vx + flow) * dt;
        d.y += (d.vy + Math.sin(d.t)*2) * dt;
        d.t += dt;
        // keep within canvas softly
        if (d.x < 0) d.x = 0; if (d.x > W()) d.x = W();
        if (d.y < 0) d.y = 0; if (d.y > H()) d.y = H();
      }
      function drawDrop(d){
        ctx.save(); ctx.translate(d.x*DPR, d.y*DPR); ctx.scale(1,1);
        const squish = 0.8 + Math.sin(d.t)*0.05;
        ctx.rotate(d.rot + Math.sin(d.t*0.7)*0.05);
        ctx.scale(0.78, squish); // thin look
        drawDropPath(d.r);
        fillDrop(d.r);
        ctx.restore();
      }

      function stepPlatelet(p, dt){
        p.x += p.vx*dt; p.y += p.vy*dt; p.t += dt*1.2;
        // recycle softly
        if (p.x < W()*0.45) { Object.assign(p, makePlatelet()); p.x = W()*0.88; }
        if (p.y < 0) p.y = H(); if (p.y > H()) p.y = 0;
      }
      function drawPlatelet(p){
        ctx.save();
        ctx.translate(p.x*DPR, p.y*DPR);
        ctx.rotate(Math.sin(p.t)*0.3);
        const rr = p.r*DPR;
        // glow
        ctx.globalCompositeOperation = 'screen';
        ctx.globalAlpha = 0.35*p.a; ctx.filter = 'blur(4px)';
        ctx.fillStyle = 'rgba(255,190,90,0.9)';
        ctx.beginPath(); ctx.ellipse(0,0, rr*1.2, rr*0.8, 0, 0, Math.PI*2); ctx.fill();
        // crisp
        ctx.filter = 'none'; ctx.globalAlpha = 0.9*p.a;
        const g = ctx.createRadialGradient(-rr*0.2,-rr*0.2, rr*0.1, 0,0, rr*1.1);
        g.addColorStop(0,'#ffd38a'); g.addColorStop(1,'#b86c24');
        ctx.fillStyle = g; ctx.beginPath(); ctx.ellipse(0,0, rr*1.0, rr*0.65, 0, 0, Math.PI*2); ctx.fill();
        ctx.restore();
      }

      let t0 = performance.now();
      let hoverFactor = 1;
      const container = canvas.closest('.hero') || canvas.parentElement || document.body;
      function onEnter(){ hoverFactor = 0.35; }
      function onLeave(){ hoverFactor = 1; }
      container.addEventListener('mouseenter', onEnter);
      container.addEventListener('mouseleave', onLeave);
      container.addEventListener('touchstart', onEnter, {passive:true});
      container.addEventListener('touchend', onLeave, {passive:true});

      function loop(now){
        const dt = Math.min(0.05, (now - t0)/1000); t0 = now;
        ctx.clearRect(0,0,canvas.width, canvas.height);
        if (cfg.mode === 'rbc'){
          if (cfg.vesselBg) drawVesselBackground();
          for (const d of cells) d.depth = d.z;
          const dtEff = (prefersReduced ? 0 : dt * cfg.speed * hoverFactor);
          cells.sort((a,b)=>b.depth-a.depth).forEach(d=>{ step(d, dtEff); draw(d); });
        } else {
          // drops mode: soft background glow only
          const w = canvas.width/DPR, h = canvas.height/DPR;
          // plasma ribbons to fill right-side empty space
          drawPlasmaRibbons(now);
          const dtEff = (prefersReduced ? 0 : dt * (0.6*cfg.speed) * hoverFactor);
          if (cfg.dropsEnabled) {
            drops.forEach(d=>{ stepDrop(d, dtEff); drawDrop(d); });
          }
          // sprinkle platelets
          if (cfg.platelets){
            platelets.forEach(p=>{ stepPlatelet(p, dtEff); drawPlatelet(p); });
          }
        }
        if (!prefersReduced) rafRef.current = requestAnimationFrame(loop);
      }
      rafRef.current = requestAnimationFrame(loop);

      // Recompute on resize or if CSS vars change externally
  function onResize(){ fit(); cfg = readVars(); if (cfg.mode==='rbc') resetCells(); else resetDrops(); }
      window.addEventListener('resize', onResize);

      return () => {
        cancelAnimationFrame(rafRef.current);
        window.removeEventListener('resize', onResize);
        container.removeEventListener('mouseenter', onEnter);
        container.removeEventListener('mouseleave', onLeave);
        container.removeEventListener('touchstart', onEnter);
        container.removeEventListener('touchend', onLeave);
      };
    }, []);

    return React.createElement('canvas', { className: 'drops-canvas', ref: canvasRef, 'aria-hidden': 'true' });
  }

  // Mount into #rbc-root
  const mountEl = document.getElementById('rbc-root');
  if (mountEl) {
    const root = ReactDOM.createRoot(mountEl);
    root.render(React.createElement(RBCBackground, {}));
  }
})();
