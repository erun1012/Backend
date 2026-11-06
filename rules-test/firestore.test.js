const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');

let env;
beforeAll(async () => {
    env = await initializeTestEnvironment({
        projectId: 'goner-ssu',
        firestore: { rules: fs.readFileSync('../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8080 },
    });
});
afterAll(() => env.cleanup());
beforeEach(() => env.clearFirestore());

describe('users', () => {
    test('비로그인은 읽기 불가', async () => {
        const db = env.unauthenticatedContext().firestore();
        await assertFails(db.doc('users/alice').get());
    });
    test('로그인하면 읽기 가능', async () => {
        const db = env.authenticatedContext('bob').firestore();
        await assertSucceeds(db.doc('users/alice').get());
    });
    test('본인 문서 생성 가능', async () => {
        const db = env.authenticatedContext('alice').firestore();
        await assertSucceeds(db.doc('users/alice').set({ nickname: 'a' }));
    });
    test('남의 문서 쓰기 불가', async () => {
        const db = env.authenticatedContext('bob').firestore();
        await assertFails(db.doc('users/alice').set({ nickname: 'hack' }));
    });
    test('본인이 누적 필드 생성 불가', async () => {
        const db = env.authenticatedContext('alice').firestore();
        await assertFails(db.doc('users/alice').set({ nickname: 'a', lateCount: 0 }));
    });
    test('본인이 누적 필드 수정 불가', async () => {
        await env.withSecurityRulesDisabled(async (ctx) => {
            await ctx.firestore().doc('users/alice').set({ nickname: 'a', lateCount: 3 });
        });
        const db = env.authenticatedContext('alice').firestore();
        await assertFails(db.doc('users/alice').update({ lateCount: 0 }));
        await assertSucceeds(db.doc('users/alice').update({ nickname: 'new' }));
    });
    test('본인 문서 삭제 불가', async () => {
        await env.withSecurityRulesDisabled(async (ctx) => {
            await ctx.firestore().doc('users/alice').set({ nickname: 'a' });
        });
        const db = env.authenticatedContext('alice').firestore();
        await assertFails(db.doc('users/alice').delete());
    });
});

describe('기본 차단', () => {
    test('정의 안 된 컬렉션은 로그인해도 접근 불가', async () => {
        const db = env.authenticatedContext('alice').firestore();
        await assertFails(db.doc('appointments/a1').get());
        await assertFails(db.doc('appointments/a1').set({ x: 1 }));
    });
});