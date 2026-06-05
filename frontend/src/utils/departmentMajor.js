export const DEPARTMENT_MAJOR_MAP = {
  计算机学院: [
    '计算机科学与技术',
    '软件工程',
    '网络工程',
    '信息安全',
    '物联网工程',
    '人工智能',
    '数据科学与大数据技术'
  ],
  电子工程学院: [
    '电子信息工程',
    '通信工程',
    '自动化',
    '微电子科学与工程',
    '电气工程及其自动化',
    '智能科学与技术'
  ],
  机械工程学院: [
    '机械工程',
    '机械设计制造及其自动化',
    '车辆工程',
    '工业设计',
    '智能制造工程'
  ],
  土木工程学院: [
    '土木工程',
    '工程管理',
    '给排水科学与工程',
    '道路桥梁与渡河工程'
  ],
  建筑学院: [
    '建筑学',
    '城乡规划',
    '风景园林'
  ],
  材料科学与工程学院: [
    '材料科学与工程',
    '高分子材料与工程',
    '金属材料工程'
  ],
  化学化工学院: [
    '化学',
    '应用化学',
    '化学工程与工艺',
    '制药工程'
  ],
  生命科学学院: [
    '生物科学',
    '生物技术',
    '生物工程',
    '食品科学与工程'
  ],
  数学与统计学院: [
    '数学与应用数学',
    '信息与计算科学',
    '统计学'
  ],
  物理学院: [
    '物理学',
    '应用物理学',
    '光电信息科学与工程'
  ],
  环境科学与工程学院: [
    '环境工程',
    '环境科学',
    '资源环境科学'
  ],
  经济管理学院: [
    '工商管理',
    '市场营销',
    '会计学',
    '金融学',
    '国际经济与贸易',
    '电子商务',
    '物流管理',
    '人力资源管理'
  ],
  公共管理学院: [
    '行政管理',
    '公共事业管理',
    '土地资源管理'
  ],
  法学院: [
    '法学',
    '知识产权'
  ],
  人文学院: [
    '汉语言文学',
    '历史学',
    '哲学',
    '新闻学',
    '广告学'
  ],
  外国语学院: [
    '英语',
    '日语',
    '法语',
    '德语',
    '翻译',
    '商务英语'
  ],
  艺术学院: [
    '视觉传达设计',
    '环境设计',
    '产品设计',
    '数字媒体艺术',
    '音乐学'
  ],
  医学院: [
    '临床医学',
    '护理学',
    '医学检验技术',
    '康复治疗学'
  ],
  药学院: [
    '药学',
    '药物制剂',
    '中药学'
  ],
  体育学院: [
    '体育教育',
    '社会体育指导与管理',
    '运动训练'
  ]
}

export const DEPARTMENT_OPTIONS = Object.keys(DEPARTMENT_MAJOR_MAP)

export const ALL_MAJORS = [...new Set(Object.values(DEPARTMENT_MAJOR_MAP).flat())]

export function getMajorsByDepartment(department) {
  if (!department) {
    return ALL_MAJORS
  }
  return DEPARTMENT_MAJOR_MAP[department] || []
}

export function getDepartmentByMajor(major) {
  if (!major) {
    return ''
  }
  for (const [department, majors] of Object.entries(DEPARTMENT_MAJOR_MAP)) {
    if (majors.includes(major)) {
      return department
    }
  }
  return ''
}

export function onDepartmentChange(form, department) {
  form.department = department
  const majors = getMajorsByDepartment(department)
  if (form.major && !majors.includes(form.major)) {
    form.major = ''
  }
}

export function onMajorChange(form, major) {
  form.major = major
  const department = getDepartmentByMajor(major)
  if (department) {
    form.department = department
  }
}
