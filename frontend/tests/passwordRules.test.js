import test from 'node:test';
import assert from 'node:assert/strict';
import {passwordRules} from '../src/passwordRules.js';
test('marca cada regla por separado mientras se escribe',()=>{
 assert.deepEqual(passwordRules('').map(r=>r.met),[false,false,false,false]);
 assert.deepEqual(passwordRules('a').map(r=>r.met),[false,false,true,false]);
 assert.deepEqual(passwordRules('Ab').map(r=>r.met),[false,true,true,false]);
 assert.deepEqual(passwordRules('Ab1').map(r=>r.met),[false,true,true,true]);
 assert.ok(passwordRules('Abcdefg1').every(r=>r.met));
});
test('admite letras españolas y cuenta caracteres completos',()=>{
 assert.ok(passwordRules('Árboles1').every(r=>r.met));
 assert.equal(passwordRules('😀😀😀😀').find(r=>r.id==='length').met,false);
});
