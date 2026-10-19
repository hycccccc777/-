<script setup>
defineProps({ type: String, background: { type: String, default: '#eeede3' } })
</script>
<template>
  <div class="product-art" :style="{ backgroundColor: background }">
    <svg viewBox="0 0 300 240" aria-hidden="true">
      <ellipse cx="150" cy="204" rx="90" ry="10" fill="#000" opacity=".06"/>
      <g v-if="type === 'math' || type === 'python'" transform="rotate(-12 150 120)">
        <rect x="86" y="41" width="127" height="165" rx="4" fill="#d5d2c3"/>
        <rect x="81" y="34" width="127" height="165" rx="4" :fill="type === 'math' ? '#e5d9bb' : '#2e6d62'"/>
        <path d="M91 35v162" stroke="#000" opacity=".13"/>
        <rect x="103" y="58" width="83" height="2" fill="#fff" opacity=".6"/>
        <text x="104" y="92" :fill="type === 'math' ? '#354238' : '#fff'" font-size="22" font-family="serif" font-weight="bold">{{ type === 'math' ? '高等数学' : 'Python' }}</text>
        <text x="104" y="113" :fill="type === 'math' ? '#53634c' : '#d5e7da'" font-size="9">{{ type === 'math' ? '同济大学 · 第七版' : '从入门到实践' }}</text>
        <path d="M108 155q20-50 32-8t33-7" fill="none" stroke="#fff" stroke-width="2" opacity=".7"/>
        <text x="104" y="182" fill="#fff" font-size="8">A NEW CHAPTER</text>
      </g>
      <g v-else-if="type === 'headphones'">
        <path d="M84 132V102a66 66 0 0 1 132 0v30" fill="none" stroke="#303437" stroke-width="18"/>
        <path d="M85 102a65 65 0 0 1 130 0" fill="none" stroke="#606669" stroke-width="6"/>
        <g transform="rotate(-12 87 145)"><rect x="63" y="113" width="44" height="76" rx="21" fill="#292d2f"/><rect x="70" y="118" width="27" height="64" rx="13" fill="#43494b"/></g>
        <g transform="rotate(12 213 145)"><rect x="193" y="113" width="44" height="76" rx="21" fill="#292d2f"/><rect x="204" y="118" width="25" height="64" rx="12" fill="#43494b"/></g>
      </g>
      <g v-else-if="type === 'lamp'">
        <ellipse cx="153" cy="201" rx="48" ry="9" fill="#c8bca2"/>
        <path d="M153 190V98" stroke="#c7b894" stroke-width="9"/>
        <path d="M102 109h102l-20-60h-61z" fill="#e4d6b4"/>
        <ellipse cx="153" cy="109" rx="51" ry="8" fill="#fbefd2"/>
        <path d="M125 52h56" stroke="#f3e7cf" stroke-width="3"/>
        <ellipse cx="153" cy="197" rx="43" ry="7" fill="#e7d7b6"/>
      </g>
      <g v-else-if="type === 'bike'">
        <circle cx="72" cy="161" r="40" fill="none" stroke="#363d39" stroke-width="6"/>
        <circle cx="229" cy="161" r="40" fill="none" stroke="#363d39" stroke-width="6"/>
        <path d="m72 161 49-73 34 73H72l44-53h88l25 53 M155 161l47-75 M193 69h22v17 M106 85h28" fill="none" stroke="#718b70" stroke-width="7" stroke-linejoin="round"/>
        <circle cx="155" cy="161" r="9" fill="#3d4941"/><path d="m155 161 13 15h14" fill="none" stroke="#3d4941" stroke-width="4"/>
      </g>
      <g v-else-if="type === 'camera'" transform="rotate(-8 150 120)">
        <rect x="75" y="70" width="151" height="117" rx="25" fill="#bbb7d0"/>
        <path d="M80 104h140" stroke="#aaa6bf" stroke-width="2"/>
        <rect x="92" y="83" width="33" height="16" rx="3" fill="#f2eee5"/>
        <circle cx="156" cy="134" r="42" fill="#d5d1e3"/><circle cx="156" cy="134" r="31" fill="#747284"/><circle cx="156" cy="134" r="21" fill="#343f4d"/><circle cx="150" cy="127" r="8" fill="#768a9d"/>
        <circle cx="205" cy="89" r="6" fill="#625e72"/>
      </g>
      <g v-else-if="type === 'bag'">
        <path d="M113 81V62a37 37 0 0 1 74 0v19" fill="none" stroke="#bda887" stroke-width="10"/>
        <path d="m91 77-12 124h146L211 77z" fill="#ddcdb1"/>
        <path d="m99 83-9 110h123L203 83" fill="none" stroke="#c7b492" stroke-width="2"/>
        <text x="151" y="137" text-anchor="middle" font-size="15" font-family="serif" fill="#5b674c">SLOW DAYS</text>
        <path d="M138 154q13 20 25-1 M151 146v26" fill="none" stroke="#738064" stroke-width="2"/>
      </g>
      <g v-else-if="type === 'cup'">
        <rect x="113" y="49" width="77" height="155" rx="23" fill="#d3c7b2"/>
        <rect x="113" y="49" width="77" height="32" rx="10" fill="#b4a58d"/>
        <path d="M123 83v90q0 17 15 19" fill="none" stroke="#ece4d5" stroke-width="6"/>
        <text x="151" y="135" text-anchor="middle" fill="#8c806c" font-size="11">DAILY</text>
      </g>
      <g v-else><rect x="90" y="70" width="120" height="130" rx="16" fill="#b5c4a9"/><path d="M125 70V52a25 25 0 0 1 50 0v18" fill="none" stroke="#60735a" stroke-width="8"/><text x="150" y="146" text-anchor="middle" font-size="15" fill="#fff">闲置好物</text></g>
    </svg>
  </div>
</template>