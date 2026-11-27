import request from '@/utils/request'

// 查询物料备案列表
export function listMaterial(query) {
  return request({
    url: '/bonded/material/list',
    method: 'get',
    params: query
  })
}

// 查询物料备案详细
export function getMaterial(id) {
  return request({
    url: '/bonded/material/' + id,
    method: 'get'
  })
}

// 新增物料备案
export function addMaterial(data) {
  return request({
    url: '/bonded/material',
    method: 'post',
    data: data
  })
}

// 修改物料备案
export function updateMaterial(data) {
  return request({
    url: '/bonded/material',
    method: 'put',
    data: data
  })
}

// 删除物料备案
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
