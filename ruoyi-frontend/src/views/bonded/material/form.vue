<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <span>{{ title }}</span>
      </template>
      <el-form ref="materialRef" :model="form" :rules="rules" label-width="120px" style="max-width: 900px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="物料编号" prop="materialCd">
              <el-input v-model="form.materialCd" placeholder="企业 ERP 物料编号" />
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
              <el-input v-model="form.materialName" placeholder="不含六类标记后缀，申报时自动拼接" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="六类标记" prop="markType">
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
                <el-radio value="1">保税料件</el-radio>
                <el-radio value="0">非保税料件</el-radio>
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
              <el-input v-model="form.calcUnit" placeholder="通关参数代码，如 004-个 035-千克" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="法定计量单位" prop="unit1">
              <el-input v-model="form.unit1" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法定第二单位" prop="unit2">
              <el-input v-model="form.unit2" placeholder="选填" />
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
              <el-select v-model="form.dutyMode" placeholder="选填" clearable style="width: 100%">
                <el-option label="1-照章征税" value="1" />
                <el-option label="2-折半征税" value="2" />
                <el-option label="3-全免" value="3" />
                <el-option label="4-特案" value="4" />
                <el-option label="5-免税" value="5" />
                <el-option label="6-保证金" value="6" />
                <el-option label="7-保函" value="7" />
                <el-option label="8-全额退税" value="8" />
                <el-option label="9-其他" value="9" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-card>
    <el-row justify="center" class="mt16">
      <el-button type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="cancel">取 消</el-button>
    </el-row>
  </div>
</template>

<script setup name="MaterialForm">
import { getMaterial, addMaterial, updateMaterial } from '@/api/bonded/material'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const markTypeOptions = [
  { value: '1', label: '待维修货物' },
  { value: '2', label: '已维修货物' },
  { value: '3', label: '无法维修货物' },
  { value: '4', label: '维修用料件' },
  { value: '5', label: '旧件或坏件' },
  { value: '6', label: '边角料' },
  { value: '8', label: '维修服务费' }
]

const form = ref({
  id: null, materialCd: null, version: '1', calcUnit: null, materialName: null, markType: null, gType: null,
  isBonded: '1', codeTs: null, gName: null, gModel: null, unit1: null, unit2: null, dutyMode: null,
  totalGrossWt: null, totalNetWt: null, remark: null
})

const rules = {
  materialCd: [{ required: true, message: '物料编号不能为空', trigger: 'blur' }],
  materialName: [{ required: true, message: '物料名称不能为空', trigger: 'blur' }],
  markType: [{ required: true, message: '请选择六类标记', trigger: 'change' }],
  gType: [{ required: true, message: '请选择料件成品标志', trigger: 'change' }],
  gModel: [{ required: true, message: '规格型号必填', trigger: 'blur' }],
  calcUnit: [{ required: true, message: '计量单位必填', trigger: 'blur' }]
}

const id = route.params.id
const title = ref(id ? '修改物料备案' : '新增物料备案')

if (id) {
  getMaterial(id).then(res => {
    form.value = res.data
  })
}

function submitForm() {
  proxy.$refs['materialRef'].validate(valid => {
    if (valid) {
      // 规范 §2.2.1：保税料件 HS 编码/商品名称必填，非保税选填（条件校验）
      if (form.value.isBonded === '1' && (!form.value.codeTs || !form.value.gName)) {
        proxy.$modal.msgError('保税料件必填 HS 编码与 HS 商品名称')
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
