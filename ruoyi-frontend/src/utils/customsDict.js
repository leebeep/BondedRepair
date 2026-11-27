/**
 * 海关参数字典（来源：docs/dock/海关-计量单位.xlsx）
 */

// 征减免税方式（dutyMode，代码 1-9/A-E 原样存储）
export const dutyModes = [
  { value: "1", label: "1-照章征税" },
  { value: "2", label: "2-折半征税" },
  { value: "3", label: "3-全免" },
  { value: "4", label: "4-特案" },
  { value: "5", label: "5-随征免性质" },
  { value: "6", label: "6-保证金" },
  { value: "7", label: "7-保函" },
  { value: "8", label: "8-折半补税" },
  { value: "9", label: "9-全额退税" },
  { value: "A", label: "A-不计征关税、增值税和消费税" },
  { value: "B", label: "B-不计征关税" },
  { value: "C", label: "C-不计征关税、增值税" },
  { value: "D", label: "D-不计征增值税和消费税" },
  { value: "E", label: "E-不计征增值税" },
]

export function dutyModeLabel(v) {
  const d = dutyModes.find(d => d.value === v)
  return d ? d.label : (v || '-')
}

// 币制（currency，3 位补零代码，如 142-人民币、502-美元）
export const currencies = [
  { value: "138", label: "138-阿联酋迪拉姆" },
  { value: "601", label: "601-澳大利亚元" },
  { value: "501", label: "501-加拿大元" },
  { value: "331", label: "331-瑞士法郎" },
  { value: "142", label: "142-人民币" },
  { value: "302", label: "302-丹麦克朗" },
  { value: "300", label: "300-欧元" },
  { value: "303", label: "303-英镑" },
  { value: "110", label: "110-港币" },
  { value: "321", label: "321-匈牙利福林" },
  { value: "112", label: "112-印度尼西亚卢比" },
  { value: "116", label: "116-日本元" },
  { value: "107", label: "107-柬埔寨瑞尔" },
  { value: "133", label: "133-韩国圆" },
  { value: "145", label: "145-哈萨克斯坦坚戈" },
  { value: "124", label: "124-蒙古图格里克" },
  { value: "121", label: "121-澳门元" },
  { value: "429", label: "429-墨西哥比索" },
  { value: "122", label: "122-马来西亚林吉特" },
  { value: "326", label: "326-挪威克朗" },
  { value: "609", label: "609-新西兰元" },
  { value: "327", label: "327-波兰兹罗提" },
  { value: "344", label: "344-俄罗斯卢布" },
  { value: "131", label: "131-沙特里亚尔" },
  { value: "330", label: "330-瑞典克朗" },
  { value: "132", label: "132-新加坡元" },
  { value: "136", label: "136-泰国铢" },
  { value: "137", label: "137-土耳其里拉" },
  { value: "502", label: "502-美元" },
  { value: "244", label: "244-南非兰特" },
]

export function currencyLabel(v) {
  const c = currencies.find(c => c.value === v)
  return c ? c.label : (v || '-')
}
