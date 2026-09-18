import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import ExcelJS from 'exceljs';
import { parseBankExcel, validateQuestion } from '../src/bankExcel.js';

async function fixture(rows, mutate) {
 const wb=new ExcelJS.Workbook();
 await wb.xlsx.load(await fs.readFile('public/modelo-banco-preguntas.xlsx'));
 const sheet=wb.getWorksheet('Preguntas');
 rows.forEach((row,index)=>{sheet.getRow(index+2).values=row;});
 mutate?.(sheet);
 return new File([await wb.xlsx.writeBuffer()],'preguntas.xlsx');
}
const valid=['¿Cuánto es 2 + 2?','La suma es cuatro.','4','5','','','','','','','','','A'];
test('el modelo está vacío y el ejemplo no se importa',async()=>{
 const file=new File([await fs.readFile('public/modelo-banco-preguntas.xlsx')],'modelo.xlsx');
 await assert.rejects(()=>parseBankExcel(file),/está vacía/);
});
test('importa y convierte una única clave correcta',async()=>{
 const [q]=await parseBankExcel(await fixture([valid]));
 assert.equal(q.statement,valid[0]);assert.equal(q.options.length,2);
 assert.equal(q.options[0].correct,true);assert.equal(q.options[1].correct,false);
});
test('informa la fila inválida sin aceptar parcialmente el archivo',async()=>{
 const bad=[...valid];bad[12]='C';
 const file=await fixture([valid,bad]);
 await assert.rejects(()=>parseBankExcel(file),/Fila 3/);
});
test('rechaza fórmulas, opciones repetidas y huecos',async()=>{
 const formula=await fixture([valid],s=>{s.getCell('C2').value={formula:'2+2',result:4};});
 await assert.rejects(()=>parseBankExcel(formula),/fórmulas/);
 const repeated=[...valid];repeated[3]='4';
 const duplicate=await fixture([repeated]);
 await assert.rejects(()=>parseBankExcel(duplicate),/distintas/);
 const gap=[...valid];gap[3]='';gap[4]='6';
 const withGap=await fixture([gap]);
 await assert.rejects(()=>parseBankExcel(withGap),/Cada opción/);
});
test('rechaza formato incorrecto, archivo grande y columnas ausentes',async()=>{
 await assert.rejects(()=>parseBankExcel(new File(['x'],'datos.csv')),/xlsx/);
 await assert.rejects(()=>parseBankExcel({name:'datos.xlsx',size:6*1024*1024}),/5 MB/);
 const missing=await fixture([valid],s=>{s.getCell('M1').value='otro';});
 await assert.rejects(()=>parseBankExcel(missing),/encabezados/);
});
test('validación manual exige una sola clave y opciones completas',()=>{
 assert.match(validateQuestion({statement:'Pregunta',options:[{text:'A',correct:false},{text:'B',correct:false}]}),/única/);
 assert.equal(validateQuestion({statement:'Pregunta',options:[{text:'A',correct:true},{text:'B',correct:false}]}),null);
});

