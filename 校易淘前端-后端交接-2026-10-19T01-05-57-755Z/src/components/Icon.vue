<script setup>
defineProps({ name: { type: String, default: 'grid' }, size: { type: [Number, String], default: 20 } })
const paths = {
 search: 'm21 21-4.4-4.4 M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
 heart: 'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8',
 bag: 'M5 7h14l2 14H3L5 7 M9 8V6a3 3 0 0 1 6 0v2',
 cart: 'M2 3h3l3 12h11l3-9H6 M9 20h.01 M18 20h.01',
 user: 'M20 21v-2a7 7 0 0 0-14 0v2 M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
 plus: 'M12 5v14 M5 12h14',
 arrow: 'M5 12h14 m-5-5 5 5-5 5',
 location: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0 M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
 check: 'm5 12 4 4L19 6',
 shield: 'M12 2 3 6v6c0 6 9 10 9 10s9-4 9-10V6L12 2 m-5 10 3 3 6-6',
 book: 'M12 5v16 M3 3c4-1 7 0 9 2 2-2 5-3 9-2v16c-4-1-7 0-9 2-2-2-5-3-9-2V3',
 monitor: 'M3 3h18v13H3V3 M12 16v5 M8 21h8',
 coffee: 'M3 8h14v8a5 5 0 0 1-5 5H8a5 5 0 0 1-5-5V8 M17 9h2a3 3 0 0 1 0 6h-2 M7 2v3 M12 2v3',
 bike: 'M8 5h3l6 12 M7 17l5-9h5 M15 4h3v4 M9 17a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M23 17a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
 grid: 'M3 3h7v7H3V3 M14 3h7v7h-7V3 M3 14h7v7H3v-7 M14 14h7v7h-7v-7',
 leaf: 'M20 3C5 1 1 10 7 17c8 7 15-1 13-14 M5 21 16 10',
 chevron: 'm9 5 7 7-7 7',
 close: 'm6 6 12 12 M6 18 18 6',
 logout: 'M9 3H3v18h6 M9 12h12 m-4-4 4 4-4 4',
 clock: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0 M12 6v6l4 2',
}
</script>
<template><svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] || paths.grid" /></svg></template>