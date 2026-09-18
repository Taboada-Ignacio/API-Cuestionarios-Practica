export function passwordRules(value){return [
 {id:'length',label:'Al menos 8 caracteres',met:Array.from(value).length>=8},
 {id:'upper',label:'Una letra mayúscula',met:/\p{Lu}/u.test(value)},
 {id:'lower',label:'Una letra minúscula',met:/\p{Ll}/u.test(value)},
 {id:'number',label:'Un número',met:/[0-9]/.test(value)},
];}
