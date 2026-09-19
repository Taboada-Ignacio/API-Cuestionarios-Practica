import test from 'node:test';
import assert from 'node:assert/strict';
import {addUniqueTag,classificationFromUser,classificationPayload,emptyClassification,normalizeAcademicName,validateClassification} from '../src/classificationRules.js';

test('la clasificación es opcional y construye valores nulos',()=>{
 const value=emptyClassification();
 assert.equal(validateClassification(value),null);
 assert.deepEqual(classificationPayload(value),{academicUniversityId:null,academicFacultyId:null,academicCareerId:null,studyYear:null,academicSubjectName:null,subjectId:null,evaluationType:null,evaluationNumber:null,tags:[]});
});

test('el número de evaluación requiere tipo y debe ser positivo',()=>{
 assert.match(validateClassification({...emptyClassification(),evaluationNumber:'2'}),/tipo/);
 assert.match(validateClassification({...emptyClassification(),evaluationType:'PARCIAL',evaluationNumber:'0'}),/mayor o igual a 1/);
 const value={...emptyClassification(),subjectId:'materia-1',evaluationType:'PARCIAL',evaluationNumber:'2',tags:['memoria']};
 assert.equal(validateClassification(value),null);
 assert.deepEqual(classificationPayload(value),{academicUniversityId:null,academicFacultyId:null,academicCareerId:null,studyYear:null,academicSubjectName:null,subjectId:'materia-1',evaluationType:'PARCIAL',evaluationNumber:2,tags:['memoria']});
});

test('normaliza el nombre manual de la materia',()=>{
 assert.equal(normalizeAcademicName('  inGenIeria   y SocIedad  '),'Ingenieria y Sociedad');
 assert.equal(classificationPayload({...emptyClassification(),academicSubjectName:'base DE datos'}).academicSubjectName,'Base de Datos');
});

test('universidad, facultad y carrera se envían como una clasificación estructurada',()=>{
 const incomplete={...emptyClassification(),academicUniversityId:'U1'};
 assert.match(validateClassification(incomplete),/universidad, facultad y carrera/i);
 const value={...emptyClassification(),academicUniversityId:'U1',academicFacultyId:'F1',academicCareerId:'C1',studyYear:'4'};
 assert.equal(validateClassification(value),null);
 assert.equal(classificationPayload(value).studyYear,4);
});

test('precarga la formación del usuario y permite partir vacío cuando no corresponde',()=>{
 assert.deepEqual(classificationFromUser({university:'U1',faculty:'F1',career:'C1'}),{
  ...emptyClassification(),academicUniversityId:'U1',academicFacultyId:'F1',academicCareerId:'C1'
 });
 assert.deepEqual(classificationFromUser({university:'NONE',faculty:'NONE',career:'NONE'}),emptyClassification());
 assert.deepEqual(classificationFromUser(null),emptyClassification());
});

test('las etiquetas eliminan espacios y evitan duplicados por mayúsculas',()=>{
 const tags=addUniqueTag([], '  Memoria   Virtual ');
 assert.deepEqual(tags,['Memoria Virtual']);
 assert.deepEqual(addUniqueTag(tags,'memoria virtual'),tags);
});
