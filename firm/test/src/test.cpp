#include <list>
#include <zephyr/ztest.h>
#include <zephyr/kernel.h>
#include "../../src/db.hpp"

using namespace dkv::therm;

static constexpr size_t HEAP_SIZE = 1024;

static K_HEAP_DEFINE(test_heap, HEAP_SIZE);

static void zassert_heap_empty() {
	struct sys_memory_stats stats;
	sys_heap_runtime_stats_get(&test_heap.heap, &stats);
	zassert_equal(stats.allocated_bytes, 0, "heap must be empty");
}

ZTEST_SUITE(thermo_suite, NULL, NULL, NULL, NULL, NULL);

struct MyNum {
	int n, &objects_cnt;

	MyNum(int n, int &objects_cnt) : n{n}, objects_cnt{objects_cnt} {
		objects_cnt++;
	}
	~MyNum() {
		objects_cnt--;
	}
};

struct AllocStat {
	int objects_cnt;
	int bad_alloc_cnt;
	int allocated_cnt;
	int match_cnt;
};

static void run_alloc_test(int rep, AllocStat *stat) {
	KHeapList<MyNum> nums{test_heap};
	try {
		for (int i = 0; i < rep; i++) {
			nums.emplace_back(i, stat->objects_cnt);
		}
	} catch (const std::bad_alloc &ex) {
		stat->bad_alloc_cnt++;
	}
	stat->allocated_cnt = stat->objects_cnt;
	int n = 0;
	for (auto &mynum: nums) {
		stat->match_cnt += (mynum.n == n);
		n++;
	}
}

// must be correct regardless of allocation errors
static void zassert_alloc_stat_invariant(const AllocStat &stat) {
	zassert_equal(stat.match_cnt, stat.allocated_cnt, "all allocated elements must be correct");
	zassert_equal(stat.objects_cnt, 0, "should destroy everything at the end");
	zassert_heap_empty();
}

ZTEST(thermo_suite, test_01_list_good_alloc) {
	AllocStat stat{};
	// each node seems to take upto 32 bytes
	constexpr int N = HEAP_SIZE / 32;
	run_alloc_test(N, &stat);
	zassert_alloc_stat_invariant(stat);
	zassert_equal(stat.bad_alloc_cnt, 0, "no bad allocs expected");
	zassert_equal(stat.allocated_cnt, N, "should allocate everything");
}

ZTEST(thermo_suite, test_02_list_over_alloc) {
	AllocStat stat{};
	constexpr int N = HEAP_SIZE;
	run_alloc_test(N, &stat);
	zassert_alloc_stat_invariant(stat);
	zassert_true(stat.bad_alloc_cnt > 0, "expecting some bad allocs");
	zassert_true(stat.allocated_cnt < N, "don't expect to allocate everything");
}
