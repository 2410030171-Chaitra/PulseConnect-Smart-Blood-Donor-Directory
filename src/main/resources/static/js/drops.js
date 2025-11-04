(function(){
  // Red Blood Cells (RBC) renderer – biconcave discs with smooth flow
  const canvas = document.getElementById('drops-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const DPR = Math.max(1, window.devicePixelRatio || 1);

  const prefersReduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function fit() {
    const cssW = canvas.clientWidth || 380;
    const cssH = canvas.clientHeight || 380;
    canvas.width = Math.floor(cssW * DPR);
    canvas.height = Math.floor(cssH * DPR);
    ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
  }
  fit();
  window.addEventListener('resize', fit);
  // ensure canvas fills its layout box and renders crisply
  canvas.style.display = canvas.style.display || 'block';
  canvas.style.width = canvas.style.width || '100%';
  canvas.style.height = canvas.style.height || '100%';
  canvas.style.touchAction = canvas.style.touchAction || 'none';

  // observe size changes (ResizeObserver where available)
  if (window.ResizeObserver) {
    const ro = new ResizeObserver(() => {
      fit();
      config = readConfig();
      resetCells();
    });
    ro.observe(canvas);
    if (container && container !== canvas) ro.observe(container);
  } else {
    // fallback polling for older browsers
    let lastW = canvas.clientWidth, lastH = canvas.clientHeight;
    setInterval(() => {
      const cw = canvas.clientWidth, ch = canvas.clientHeight;
      if (cw !== lastW || ch !== lastH) {
        lastW = cw; lastH = ch;
        fit();
        config = readConfig();
        resetCells();
      }
    }, 300);
  }

  // watch for CSS variable / root attribute changes to update config
  if (window.MutationObserver) {
    const mo = new MutationObserver(() => {
      config = readConfig();
      resetCells();
    });
    mo.observe(document.documentElement, { attributes: true, attributeFilter: ['style', 'class'] });
  }

  // respond to reduced-motion preference changes (best-effort)
  if (window.matchMedia) {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    mq.addEventListener?.('change', () => {
      // a full page repaint or recreation keeps behavior consistent with user prefs
      config = readConfig();
      resetCells();
    });
  }
  // Palette from CSS variables with fallbacks (deeper hemoglobin reds)
  const root = getComputedStyle(document.documentElement);
  // Allow customization via CSS variables (with sensible defaults)
  const crimson = (root.getPropertyValue('--rbc-body').trim() || root.getPropertyValue('--primary-color').trim() || '#dc143c');
  const rimBright = (root.getPropertyValue('--rbc-rim-bright').trim() || root.getPropertyValue('--primary-light').trim() || '#ff4d6d');
  const rimDark = (root.getPropertyValue('--rbc-rim-dark').trim() || root.getPropertyValue('--primary-dark').trim() || '#8b0e16');
  const coreDark = '#5b0a0f';

  const container = canvas.closest('.hero') || canvas.parentElement || document.body;
  // Config from CSS vars for speed/count and optional vessel background
  function readConfig(){
    const r = getComputedStyle(document.documentElement);
    const speedVar = parseFloat(r.getPropertyValue('--rbc-speed'));
    const countVar = parseInt(r.getPropertyValue('--rbc-count'));
    const vb = (r.getPropertyValue('--rbc-vessel-bg').trim() || '0');
    const vesselBg = vb === '1' || vb.toLowerCase() === 'true';
    const baseCount = isNaN(countVar) ? (W() < 640 ? 10 : 18) : countVar;
    const baseSpeed = isNaN(speedVar) ? 1 : Math.max(0.1, speedVar);
    return { speed: baseSpeed, count: baseCount, vesselBg };
  }
  let config = readConfig();

  // Draw a biconcave RBC disc using gradients (ellipse)
  function drawRBC(ctx, rx, ry){
    // Base fill: rich red body with darker rim
    const base = ctx.createRadialGradient(0, 0, Math.min(rx,ry)*0.25, 0, 0, Math.max(rx,ry));
    base.addColorStop(0.00, crimson);
    base.addColorStop(0.70, crimson);
    base.addColorStop(1.00, rimDark);
    ctx.fillStyle = base;
    ctx.beginPath();
    ctx.ellipse(0, 0, rx, ry, 0, 0, Math.PI*2);
    ctx.fill();

    // Central pallor (RBC center appears lighter)
    ctx.save();
    ctx.globalCompositeOperation = 'screen';
    const pallor = ctx.createRadialGradient(0, 0, 0, 0, 0, Math.min(rx,ry)*0.55);
    pallor.addColorStop(0.00, 'rgba(255,235,235,0.55)');
    pallor.addColorStop(1.00, 'rgba(255,235,235,0)');
    ctx.fillStyle = pallor;
    ctx.beginPath();
    ctx.ellipse(0, 0, rx*0.7, ry*0.7, 0, 0, Math.PI*2);
    ctx.fill();
    ctx.restore();

    // Rim darkening for thickness (multiply)
    ctx.save();
    ctx.globalCompositeOperation = 'multiply';
    const rimShade = ctx.createRadialGradient(0, 0, Math.min(rx,ry)*0.6, 0, 0, Math.max(rx,ry));
    rimShade.addColorStop(0.00, 'rgba(0,0,0,0)');
    rimShade.addColorStop(1.00, 'rgba(0,0,0,0.35)');
    ctx.fillStyle = rimShade;
    ctx.beginPath();
    ctx.ellipse(0, 0, rx, ry, 0, 0, Math.PI*2);
    ctx.fill();
    ctx.restore();

    // Subtle edge highlight to avoid plastic look
    ctx.save();
    ctx.globalCompositeOperation = 'screen';
    const hl = ctx.createLinearGradient(-rx, -ry, rx, ry);
    hl.addColorStop(0.50, 'rgba(255,255,255,0)');
    hl.addColorStop(0.58, 'rgba(255,255,255,0.22)');
    hl.addColorStop(0.66, 'rgba(255,255,255,0)');
    ctx.strokeStyle = hl;
    ctx.lineWidth = Math.max(1, Math.min(rx,ry)*0.06);
    ctx.beginPath();
    ctx.ellipse(0, 0, rx*0.96, ry*0.96, 0, 0, Math.PI*2);
    ctx.stroke();
    ctx.restore();
  }

  function shadow(ctx, r){
    ctx.save();
    ctx.fillStyle = 'rgba(0,0,0,0.35)';
    ctx.filter = 'blur(' + Math.max(6, r*0.20) + 'px)';
    ctx.beginPath();
    ctx.ellipse(0, r*1.1, r*0.7, r*0.25, 0, 0, Math.PI*2);
    ctx.fill();
    ctx.restore();
  }

  const W = () => canvas.clientWidth || 380;
  const H = () => canvas.clientHeight || 380;

  // Vessel background with bright far end and darker walls (optional)
  function drawVesselBackground(){
    const w = canvas.width, h = canvas.height;
    const cx = w/2, cy = h/2;
    const r = Math.hypot(cx, cy);
    const g = ctx.createRadialGradient(cx, cy, r*0.06, cx, cy, r*1.05);
    g.addColorStop(0.00, 'rgba(255,235,235,0.95)');
    g.addColorStop(0.25, 'rgba(255,120,120,0.55)');
    g.addColorStop(1.00, 'rgba(90,0,30,0.95)');
    ctx.fillStyle = g;
    ctx.fillRect(0,0,w,h);

    // faint spiral ridges
    ctx.save();
    ctx.translate(cx, cy);
    ctx.globalAlpha = 0.08;
    const maxR = r*0.92;
    for(let i=0;i<6;i++){
      const ang = (i/6)*Math.PI*2 + performance.now()/7000;
      ctx.rotate(ang);
      const ring = ctx.createRadialGradient(0,0, maxR*0.25, 0,0, maxR);
      ring.addColorStop(0, 'rgba(255,255,255,0)');
      ring.addColorStop(1, 'rgba(255,255,255,0.9)');
      ctx.strokeStyle = ring;
      ctx.lineWidth = Math.max(1, Math.min(w,h)*0.02);
      ctx.beginPath();
      ctx.ellipse(0, 0, maxR*0.88, maxR*0.62, 0, 0, Math.PI*2);
      ctx.stroke();
    }
    ctx.restore();
  }

  // RBC with helical 3D vessel flow
  function makeCell(depth){
    const rBase = Math.min(W(), H()) * 0.075;
    const r = rBase * (0.6 + depth*0.6); // base physical radius
    return {
      // cylindrical coords around vessel axis
      theta: Math.random()*Math.PI*2,
      z: (0.2 + Math.random()*0.8) * 1200, // 0=near, large=far
      rCyl: (0.25 + Math.random()*0.6) * Math.min(W(),H())*0.38,
      omega: (0.3 + Math.random()*0.6) * (Math.random()<0.5?-1:1),
      vz: 60 * (0.7 + Math.random()*0.6),
      r,
      rot: Math.random()*Math.PI,
      t: Math.random()*Math.PI*2,
      wobble: 0.10 + Math.random()*0.15,
      aspect: 0.55 + Math.random()*0.15,
      depth: 1 // updated per-frame
    };
  }

  let cells = [];
  function resetCells(){
    cells = [];
    // cell count based on config (responsive)
    const n = config.count;
    for (let i=0;i<n;i++) cells.push(makeCell(0.4 + Math.random()*0.9));
  }
  resetCells();

  function step(d, dt){
    // Helical motion down the vessel towards the viewer
    d.theta += d.omega * dt;
    d.z -= d.vz * dt; // move towards camera
    d.t += dt * 0.7;  // gentle wobble
    if (d.z < 60) { // passed viewer; respawn far end
      const depth = 0.4 + Math.random()*0.9;
      Object.assign(d, makeCell(depth));
    }
  }

  function draw(d){
    ctx.save();
    const w = canvas.width, h = canvas.height;
    const cx = w/2, cy = h/2;
    // Perspective parameters
    const FOV = 520; // bigger -> weaker perspective
    const scale = FOV / (FOV + d.z);

    // World position (cylindrical coords)
    const xw = d.rCyl * Math.cos(d.theta);
    const yw = d.rCyl * Math.sin(d.theta) * 0.75; // vessel cross-section squash

    // Project
    const sx = cx + xw * scale;
    const sy = cy + yw * scale;
    ctx.translate(sx, sy);

    // Size and orientation
    const wob = 1 + Math.sin(d.t + d.theta)*d.wobble*0.3;
    const rp = d.r * scale * 1.2; // slightly larger to feel close
    const rx = rp * wob;
    const ry = rp * (d.aspect * (2 - wob));
    ctx.rotate(d.rot + Math.sin(d.t*0.6)*0.25);

  // Soft drop shadow to sit nicely on purple background
  ctx.save();
  ctx.globalCompositeOperation = 'multiply';
  ctx.globalAlpha = 0.20 * scale;
  ctx.filter = `blur(${(2 + (1-scale)*4).toFixed(2)}px)`;
  ctx.beginPath();
  ctx.ellipse(rx*0.05, ry*0.55, rx*0.6, ry*0.25, 0, 0, Math.PI*2);
  ctx.fillStyle = 'rgba(0,0,0,1)';
  ctx.fill();
  ctx.restore();

  // Depth-based blur and alpha (DOF)
    const blurPx = (1 - scale) * 2.2;
    ctx.filter = `blur(${blurPx.toFixed(2)}px)`;
    ctx.globalAlpha = 0.65 + 0.35*scale;

    // draw cell body
    drawRBC(ctx, rx, ry);
    ctx.filter = 'none';
    ctx.globalAlpha = 1;
    ctx.restore();
  }

  let t0 = performance.now();
  let hoverFactor = 1; // slow down on hover

  // Hover/touch slow-down for accessibility
  function onEnter(){ hoverFactor = 0.35; }
  function onLeave(){ hoverFactor = 1; }
  container.addEventListener('mouseenter', onEnter);
  container.addEventListener('mouseleave', onLeave);
  container.addEventListener('touchstart', onEnter, {passive: true});
  container.addEventListener('touchend', onLeave, {passive: true});

  // Recompute on resize (size + responsive count)
  window.addEventListener('resize', () => { 
    config = readConfig(); 
    resetCells();
  });

  function frame(now){
    const dt = Math.min(0.05, (now - t0) / 1000); t0 = now;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if (config.vesselBg) drawVesselBackground();
    // derive depth for sorting (larger z is further)
    for (const d of cells) d.depth = d.z;
    const dtEff = (prefersReduced ? 0 : dt*config.speed*hoverFactor);
    cells.sort((a,b)=>b.depth-a.depth).forEach(d => { step(d, dtEff); draw(d); });
    if (!prefersReduced) requestAnimationFrame(frame);
  }

  requestAnimationFrame(frame);
})();
