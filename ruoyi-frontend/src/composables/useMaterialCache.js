import { ref } from 'vue'
import { listMaterial } from '@/api/bonded/material'

// ===== 全局共享物料档案缓存 =====
// 规范：列表/明细/表单统一经此读取备案通过（declareStatus='2'）的物料档案。
// 避免各页面分散调用 listMaterial 且分页口径不一（曾出现 200/500 混用），
// 导致 materialId → 物料编号 映射在物料较多时静默缺失。
const materials = ref([])
const byIdMap = ref({})
let loaded = false
let pending = null

export function useMaterialCache() {
  function load() {
    if (loaded) return Promise.resolve(materials.value)
    if (!pending) {
      pending = listMaterial({ pageNum: 1, pageSize: 2000, declareStatus: '2' }).then(r => {
        materials.value = r.rows || []
        byIdMap.value = materials.value.reduce((m, item) => { m[item.id] = item; return m }, {})
        loaded = true
        pending = null
        return materials.value
      }).catch(e => { pending = null; throw e })
    }
    return pending
  }

  /** materialId → 物料编号（展示回退规范：优先行内 materialCd，缺失时用档案） */
  function cdOf(materialId) { return byIdMap.value[materialId]?.materialCd || null }
  /** materialId → 物料档案对象（编号/名称/计量单位/规格/保税标志等） */
  function byId(materialId) { return byIdMap.value[materialId] || null }

  return { materials, load, cdOf, byId }
}
