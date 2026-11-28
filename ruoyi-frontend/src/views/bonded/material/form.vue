<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header-row"><span>{{ title }}</span><el-button link type="primary" icon="Back" @click="cancel">返回列表</el-button></div>
      </template>
      <el-form ref="materialRef" :model="form" :rules="rules" label-width="120px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编号" prop="materialCd">
              <el-input v-model="form.materialCd" :disabled="cdDisabled" :placeholder="cdPlaceholder" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="form.version" placeholder="默认 1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="materialName">
              <el-input v-model="form.materialName" placeholder="不含物料属性后缀，申报时自动拼接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料属性" prop="markType">
              <el-select v-model="form.markType" placeholder="请选择" style="width: 100%">
                <el-option v-for="d in markTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="料件成品标志" prop="gType">
              <el-select v-model="form.gType" placeholder="请选择" style="width: 100%">
                <el-option label="I-料件" value="I" />
                <el-option label="E-成品" value="E" />
                <el-option label="S-半成品" value="S" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否保税" prop="isBonded">
              <el-radio-group v-model="form.isBonded">
                <el-radio value="1">保税</el-radio>
                <el-radio value="0">非保税</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">海关申报信息</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="HS编码" prop="codeTs">
              <el-input v-model="form.codeTs" :placeholder="form.isBonded === '1' ? '保税必填' : '选填'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="HS商品名称" prop="gName">
              <el-input v-model="form.gName" :placeholder="form.isBonded === '1' ? '保税必填' : '选填'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="规格型号" prop="gModel">
              <el-input v-model="form.gModel" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位" prop="calcUnit">
              <el-select v-model="form.calcUnit" filterable placeholder="请选择" style="width: 100%">
                <el-option v-for="u in customsUnits" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="法定计量单位" prop="unit1">
              <el-select v-model="form.unit1" filterable clearable :placeholder="form.isBonded === '1' ? '保税必填' : '选填'" style="width: 100%">
                <el-option v-for="u in customsUnits" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定第二单位" prop="unit2">
              <el-select v-model="form.unit2" filterable clearable placeholder="选填" style="width: 100%">
                <el-option v-for="u in customsUnits" :key="u.value" :label="u.label" :value="u.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="货物毛重" prop="totalGrossWt">
              <el-input v-model="form.totalGrossWt" placeholder="选填，如 1200.11111" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="货物净重" prop="totalNetWt">
              <el-input v-model="form.totalNetWt" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="征免方式" prop="dutyMode">
              <el-select v-model="form.dutyMode" filterable placeholder="选填" clearable style="width: 100%">
                <el-option v-for="d in dutyModes" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>
    <el-row justify="end" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="MaterialForm">
import { getMaterial, addMaterial, updateMaterial, getMaterialCdMode } from '@/api/bonded/material'
import { customsUnits } from '@/utils/customsUnits'
import { materialMarkTypes as markTypeOptions } from '@/utils/bonded'
import { dutyModes } from '@/utils/customsDict'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const form = ref({
  id: null, materialCd: null, version: '1', calcUnit: null, materialName: null, markType: null, gType: null,
  isBonded: '1', codeTs: null, gName: null, gModel: null, unit1: null, unit2: null, dutyMode: null,
  totalGrossWt: null, totalNetWt: null, remark: null
})

const rules = computed(() => ({
  materialCd: [{ required: !autoNo.value, message: '物料编号不能为空', trigger: 'blur' }],
  materialName: [{ required: true, message: '物料名称不能为空', trigger: 'blur' }],
  markType: [{ required: true, message: '请选择物料属性', trigger: 'change' }],
  gType: [{ required: true, message: '请选择料件成品标志', trigger: 'change' }],
  gModel: [{ required: true, message: '规格型号必填', trigger: 'blur' }],
  calcUnit: [{ required: true, message: '计量单位必填', trigger: 'change' }],
  // 规范 §2.2.1：保税货物法定计量单位必填，非保税选填
  unit1: [{ required: form.value.isBonded === '1', message: '保税货物必填法定计量单位', trigger: 'change' }]
}))

const id = route.params.id
const title = ref(id ? '修改物料清单' : '新增物料清单')

// 物料编号模式：自动生成（参数 br.material.auto.no，默认开）则禁用手输；编辑时编号固定
const autoNo = ref(false)
const cdDisabled = computed(() => !!id || autoNo.value)
const cdPlaceholder = computed(() => (id ? '编号不可修改' : autoNo.value ? '保存后自动生成（WL+日期+流水）' : '企业 ERP 物料编号'))
getMaterialCdMode().then(r => { autoNo.value = r.data === true })

if (id) {
  getMaterial(id).then(res => {
    form.value = res.data
  })
}

function submitForm() {
  proxy.$refs['materialRef'].validate(valid => {
    if (valid) {
      // 规范 §2.2.1：保税货物 HS 编码/商品名称必填，非保税选填（条件校验）
      if (form.value.isBonded === '1' && (!form.value.codeTs || !form.value.gName)) {
        proxy.$modal.msgError('保税货物必填 HS 编码与 HS 商品名称')
        return
      }
      if (form.value.isBonded === '1' && !form.value.unit1) {
        proxy.$modal.msgError('保税货物必填法定计量单位')
        return
      }
      const req = form.value.id ? updateMaterial(form.value) : addMaterial(form.value)
      req.then(() => {
        proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
        proxy.$tab.closeOpenPage({ path: '/bonded/material' })
      })
    }
  })
}

function cancel() {
  proxy.$tab.closeOpenPage({ path: '/bonded/material' })
}
</script>
