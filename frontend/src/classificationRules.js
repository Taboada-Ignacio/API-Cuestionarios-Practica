export const emptyClassification=()=>({academicUniversityId:'',academicFacultyId:'',academicCareerId:'',studyYear:'',academicSubjectName:'',subjectId:'',evaluationType:'',evaluationNumber:'',tags:[]});
const lowercaseWords=new Set(['a','al','con','de','del','el','en','e','la','las','los','o','para','por','sin','u','y']);
export function normalizeAcademicName(name){
 return name.trim().replace(/\s+/g,' ').toLocaleLowerCase('es').split(' ').map((word,index)=>index>0&&lowercaseWords.has(word)?word:word.charAt(0).toLocaleUpperCase('es')+word.slice(1)).join(' ');
}
export function classificationFromUser(user){
 const values=[user?.university,user?.faculty,user?.career];
 const valid=values.every(value=>value&&value!=='NONE'&&value!=='MISSING');
 return {...emptyClassification(),...(valid?{academicUniversityId:values[0],academicFacultyId:values[1],academicCareerId:values[2]}:{})};
}
export function validateClassification(value){
 const academic=[value.academicUniversityId,value.academicFacultyId,value.academicCareerId];
 if(academic.some(Boolean)&&!academic.every(Boolean))return 'Completá universidad, facultad y carrera, o dejá los tres campos vacíos.';
 if(value.studyYear!==''&&!value.academicCareerId)return 'Elegí una carrera antes de indicar el año.';
 if(value.evaluationNumber!==''&&!value.evaluationType)return 'Elegí un tipo de evaluación antes de indicar el número.';
 if(value.evaluationNumber!==''&&(!Number.isInteger(Number(value.evaluationNumber))||Number(value.evaluationNumber)<1))return 'El número de evaluación debe ser un entero mayor o igual a 1.';
 if(value.tags.length>30)return 'Podés agregar hasta 30 etiquetas.';
 return null;
}
export function classificationPayload(value){return {academicUniversityId:value.academicUniversityId||null,academicFacultyId:value.academicFacultyId||null,academicCareerId:value.academicCareerId||null,studyYear:value.studyYear===''?null:Number(value.studyYear),academicSubjectName:value.academicSubjectName?normalizeAcademicName(value.academicSubjectName):null,subjectId:value.subjectId||null,evaluationType:value.evaluationType||null,evaluationNumber:value.evaluationNumber===''?null:Number(value.evaluationNumber),tags:value.tags};}
export function addUniqueTag(tags,name){const clean=name.trim().replace(/\s+/g,' ');if(!clean||tags.some(tag=>tag.localeCompare(clean,undefined,{sensitivity:'accent'})===0))return tags;return [...tags,clean];}
