import request from '@/utils/request'

// 查询物料清单列表
export function listMaterial(query) {
  return request({
    url: '/bonded/material/list',
    method: 'get',
    params: query
  })
}

// 查询物料清单详细
export function getMaterial(id) {
  return request({
    url: '/bonded/material/' + id,
    method: 'get'
  })
}

// 新增物料清单
export function addMaterial(data) {
  return request({
    url: '/bonded/material',
    method: 'post',
    data: data
  })
}

// 修改物料清单
export function updateMaterial(data) {
  return request({
    url: '/bonded/material',
    method: 'put',
    data: data
  })
}

// 删除物料清单
export function delMaterial(id) {
  return request({
    url: '/bonded/material/' + id,
    method: 'delete'
  })
}

// 提交 EPD101 申报 executeType: 1-备案 2-变更 3-作废
export function declareMaterial(executeType, ids) {
  return request({
    url: `/bonded/material/declare/${executeType}/${ids}`,
    method: 'post'
  })
}

// 物料编号是否自动生成（参数 br.material.auto.no）
export function getMaterialCdMode() {
  return request({
    url: '/bonded/material/cd-mode',
    method: 'get'
  })
}
