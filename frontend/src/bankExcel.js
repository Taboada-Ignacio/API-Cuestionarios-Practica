export const LETTERS = 'ABCDEFGHIJ'.split('');
export const HEADERS = ['enunciado','explicacion',...LETTERS.map(l=>'opcion_'+l.toLowerCase()),'correcta'];

export function validateQuestion(q) {
  if (!q.statement.trim() || q.statement.trim().length > 20000) return 'El enunciado es obligatorio y admite hasta 20.000 caracteres.';
  if ((q.explanation || '').length > 20000) return 'La explicación admite hasta 20.000 caracteres.';
  if (!q.options || q.options.length < 2 || q.options.length > 10) return 'Agregá entre 2 y 10 opciones.';
  if (q.options.some(o=>!o.text.trim() || o.text.trim().length>1000)) return 'Cada opción debe tener entre 1 y 1.000 caracteres.';
  if (new Set(q.options.map(o=>o.text.trim().toLocaleLowerCase('es'))).size !== q.options.length) return 'Las opciones deben ser distintas.';
  if (q.options.filter(o=>o.correct).length !== 1) return 'Marcá una única respuesta correcta.';
  return null;
}

function cellText(cell) {
  const value=cell.value;
  if(value == null) return '';
  if(typeof value==='string' || typeof value==='number' || typeof value==='boolean') return String(value).trim();
  if(value.richText) return value.richText.map(t=>t.text).join('').trim();
  throw new Error('Usá texto simple; no se admiten fórmulas, fechas ni enlaces.');
}

export async function parseBankExcel(file) {
  if(!/\.xlsx$/i.test(file.name)) throw new Error('Seleccioná un archivo Excel .xlsx.');
  if(file.size>5*1024*1024) throw new Error('El archivo debe pesar como máximo 5 MB.');
  const {default: ExcelJS}=await import('exceljs');
  const workbook=new ExcelJS.Workbook();
  try { await workbook.xlsx.load(await file.arrayBuffer()); }
  catch { throw new Error('No pudimos leer el archivo. Guardalo como .xlsx y volvé a intentar.'); }
  const sheet=workbook.getWorksheet('Preguntas');
  if(!sheet) throw new Error('Falta la hoja “Preguntas”. Usá la plantilla modelo.');
  const columns={};
  sheet.getRow(1).eachCell((cell,index)=>{
    const header=cellText(cell).toLowerCase();
    if(columns[header]) throw new Error('Hay encabezados repetidos: '+header);
    columns[header]=index;
  });
  if(HEADERS.some(h=>!columns[h])) throw new Error('Los encabezados no coinciden con la plantilla. Conservá todas las columnas, aunque algunas queden vacías.');
  if(sheet.rowCount>1001) throw new Error('Se permiten hasta 1.000 filas de preguntas. Eliminá las filas sobrantes.');
  const questions=[], errors=[];
  for(let rowIndex=2;rowIndex<=sheet.rowCount;rowIndex++) {
    try {
      const row=sheet.getRow(rowIndex);
      const values=Object.fromEntries(HEADERS.map(h=>[h,cellText(row.getCell(columns[h]))]));
      if(Object.values(values).every(v=>!v)) continue;
      const texts=LETTERS.map(l=>values['opcion_'+l.toLowerCase()]);
      const last=texts.findLastIndex(Boolean);
      const correct=values.correcta.toUpperCase();
      if(!LETTERS.includes(correct) || LETTERS.indexOf(correct)>last) throw new Error('“correcta” debe indicar una letra A..J de una opción cargada.');
      const q={statement:values.enunciado,explanation:values.explicacion,
        options:texts.slice(0,last+1).map((text,i)=>({text,correct:LETTERS[i]===correct}))};
      const error=validateQuestion(q);
      if(error) throw new Error(error);
      questions.push(q);
    } catch(error) { errors.push('Fila '+rowIndex+': '+error.message); }
  }
  if(errors.length) throw new Error(errors.slice(0,12).join('\n')+(errors.length>12?'\nY '+(errors.length-12)+' filas más.':''));
  if(!questions.length) throw new Error('La hoja “Preguntas” está vacía. Completala desde la fila 2; el ejemplo está en otra hoja.');
  return questions;
}
