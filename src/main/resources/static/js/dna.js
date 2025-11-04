(function(){
  // Realistic double-helix rendered on canvas with depth shading.
  const canvas = document.getElementById('dna-canvas');
  if(!canvas) return;
  const ctx = canvas.getContext('2d');

  // Respect reduced motion
  const prefersReduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Color palette that adapts to section background using CSS variables (fallbacks provided)
  const root = document.documentElement;
  const getVar = (n, fb) => getComputedStyle(root).getPropertyValue(n).trim() || fb;
  // Match the hero palette (indigo → purple) softly so it blends on white background
  const colorA = getVar('--dna-color-1', '#7b88ff');
  const colorB = getVar('--dna-color-2', '#b38bff');
  const rungColor = getVar('--dna-rung-color', 'rgba(200,210,255,0.6)');

  // Allow theme overrides on donor section specifically
  const donorSection = document.getElementById('donors');
  if (donorSection) {
    const sectionStyles = getComputedStyle(donorSection);
    const sA = sectionStyles.getPropertyValue('--dna-color-1').trim();
    const sB = sectionStyles.getPropertyValue('--dna-color-2').trim();
    if (sA) root.style.setProperty('--dna-color-1', sA);
    if (sB) root.style.setProperty('--dna-color-2', sB);
  }

  let w, h, dpr;
  function resize(){
    dpr = Math.max(1, Math.min(2, window.devicePixelRatio || 1));
    const cssWidth  = canvas.clientWidth  || 1200;
    const cssHeight = canvas.clientHeight || 220;
    w = Math.floor(cssWidth * dpr);
    h = Math.floor(cssHeight * dpr);
    canvas.width = w; canvas.height = h;
    ctx.setTransform(dpr,0,0,dpr,0,0);
  }
  resize();
  window.addEventListener('resize', resize);

  // Parameters for realism
  const points = 120;          // number of segments along the helix
  const amplitude = 36;        // vertical swing
  const midY = 70;             // baseline for helix center
  const depth = 0.9;           // depth factor (0..1)
  const radiusBase = 2.5;      // base bead radius
  const stride = 10;           // pixel spacing between columns (scaled later)

  // Helper: gradient between colors based on position
  function lerpColor(a,b,t){
    const pa = parseColor(a), pb = parseColor(b);
    const mix = (x,y)=>Math.round(x + (y-x)*t);
    return `rgba(${mix(pa[0],pb[0])},${mix(pa[1],pb[1])},${mix(pa[2],pb[2])},${(pa[3]+(pb[3]-pa[3])*t).toFixed(3)})`;
  }
  function parseColor(c){
    // Accept hex or rgba
    if(c.startsWith('#')){
      const hex = c.length===4 ? c.replace(/./g,(m,i)=> i? m+m: '#') : c;
      const n = parseInt(hex.replace('#',''),16);
      return [(n>>16)&255,(n>>8)&255,n&255,1];
    }
    const m = c.match(/rgba?\(([^)]+)\)/);
    if(m){
      const p = m[1].split(',').map(v=>parseFloat(v));
      const [r,g,b,a=1] = p;
      return [r,g,b,a];
    }
    return [123,136,255,1];
  }

  // Draw one frame
  let t = 0;
  function drawFrame(){
    const cw = canvas.clientWidth || 1200;
    const ch = canvas.clientHeight || 220;
    ctx.clearRect(0,0,cw,ch);

    // soft background blend to match the page; unique subtle vignette
    ctx.save();
    const gbg = ctx.createLinearGradient(0,0,cw,0);
    gbg.addColorStop(0, 'rgba(100,110,255,0.06)');
    gbg.addColorStop(1, 'rgba(190,150,255,0.06)');
    ctx.fillStyle = gbg;
    ctx.fillRect(0,0,cw,ch);
    ctx.restore();

    // Compute horizontal spacing responsive to width
    const stepX = Math.max(6, cw / points);
    const phaseSpeed = 0.03; // animation speed

    // Two strands: +phase and -phase, with depth shading (front/back)
    for (let pass=0; pass<2; pass++){
      const isFront = pass===1; // draw back first, then front for overlap
      for (let i=0;i<=points;i++){
        const x = i*stepX;
        const phase = (i*0.22) + t;
        const yA = midY + Math.sin(phase) * amplitude;
        const yB = midY - Math.sin(phase) * amplitude;
        const z = (Math.cos(phase) * 0.5 + 0.5); // 0(back) .. 1(front)
        const r = radiusBase + z*2.2;
        const alpha = 0.35 + z*0.65;

        // Only draw current depth pass
        if ((isFront && z < 0.5) || (!isFront && z >= 0.5)) continue;

        // Strand colors blend along x for uniqueness
        const cStrand = lerpColor(colorA, colorB, i/points);
        ctx.fillStyle = cStrand.replace('rgb','rgba').replace(')',`,`+alpha.toFixed(3)+`)`);
        ctx.shadowColor = cStrand;
        ctx.shadowBlur = 8*z;

        // Beads for A strand
        ctx.beginPath();
        ctx.arc(x, yA, r, 0, Math.PI*2);
        ctx.fill();

        // Beads for B strand
        ctx.beginPath();
        ctx.arc(x, yB, r*0.9, 0, Math.PI*2);
        ctx.fill();

        // Rungs with soft glow (between yA and yB)
        if (i % 2 === 0){
          ctx.save();
          ctx.strokeStyle = rungColor;
          ctx.lineWidth = 1.5 + z*1.2;
          ctx.globalAlpha = 0.25 + z*0.35;
          ctx.beginPath();
          ctx.moveTo(x, yA);
          ctx.lineTo(x, yB);
          ctx.stroke();
          ctx.restore();
        }
      }
    }

    // Occasional nucleotide glow pulse traveling across (unique accent)
    const pulseX = ((t*40) % (cw+60)) - 30;
    ctx.save();
    const pulseGrad = ctx.createRadialGradient(pulseX, midY, 0, pulseX, midY, 120);
    pulseGrad.addColorStop(0, 'rgba(255,255,255,0.08)');
    pulseGrad.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = pulseGrad;
    ctx.fillRect(0,0,cw,ch);
    ctx.restore();

    if(!prefersReduced){ t += phaseSpeed; requestAnimationFrame(drawFrame); }
  }

  // Initial one-off render (for reduced motion too)
  drawFrame();
})();
