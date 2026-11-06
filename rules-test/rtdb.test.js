const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');

let env;
beforeAll(async () => {
    env = await initializeTestEnvironment({
        projectId: 'goner-ssu',
        database: {
            rules: fs.readFileSync('../database.rules.json', 'utf8'),
            host: '127.0.0.1',
            port: 9000,
        },
    });
});
afterAll(() => env.cleanup());
beforeEach(async () => {
    await env.clearDatabase();
    await env.withSecurityRulesDisabled(async (ctx) => {
        await ctx.database().ref('members/appt1').set({ alice: true, bob: true });
    });
});

const pos = { lat: 37.5, lng: 127.0 };

describe('locations', () => {
    test('참여자 본인 위치 쓰기 가능', async () => {
        const db = env.authenticatedContext('alice').database();
        await assertSucceeds(db.ref('locations/appt1/alice').set(pos));
    });
    test('남의 위치 쓰기 불가', async () => {
        const db = env.authenticatedContext('bob').database();
        await assertFails(db.ref('locations/appt1/alice').set(pos));
    });
    test('참여자가 아니면 쓰기 불가', async () => {
        const db = env.authenticatedContext('eve').database();
        await assertFails(db.ref('locations/appt1/eve').set(pos));
    });
    test('같은 약속 참여자는 읽기 가능', async () => {
        const db = env.authenticatedContext('bob').database();
        await assertSucceeds(db.ref('locations/appt1').get());
    });
    test('참여자가 아니면 읽기 불가', async () => {
        const db = env.authenticatedContext('eve').database();
        await assertFails(db.ref('locations/appt1').get());
    });
    test('비로그인 읽기 불가', async () => {
        const db = env.unauthenticatedContext().database();
        await assertFails(db.ref('locations/appt1').get());
    });
    test('범위를 벗어난 좌표는 불가', async () => {
        const db = env.authenticatedContext('alice').database();
        await assertFails(db.ref('locations/appt1/alice').set({ lat: 999, lng: 127 }));
    });
    test('members는 클라이언트가 쓸 수 없다', async () => {
        const db = env.authenticatedContext('eve').database();
        await assertFails(db.ref('members/appt1/eve').set(true));
    });
});