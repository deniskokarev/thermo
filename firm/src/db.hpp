#pragma once

namespace dkv::therm {

template<typename T>
class KHeapAllocator {
protected:
	k_heap &heap;
public:
	// Required typedefs
	using value_type = T;
	using pointer = T *;
	using const_pointer = const T *;
	using reference = T &;
	using const_reference = const T &;
	using size_type = std::size_t;
	using difference_type = std::ptrdiff_t;

	pointer allocate(size_type n) {
		auto ptr = k_heap_alloc(&heap, n * sizeof(T), K_NO_WAIT);
		if (!ptr) {
			throw std::bad_alloc();
		}
		return static_cast<pointer>(ptr);
	}

	inline void deallocate(pointer p, size_type n) {
		k_heap_free(&heap, p);
	}

	KHeapAllocator(k_heap &heap) : heap{heap} {
	};

	template<typename U> friend
	class KHeapAllocator;

	template<typename U>
	KHeapAllocator(const KHeapAllocator<U> &o) : KHeapAllocator(o.heap) {
	};
};

template<typename T> using KHeapList = std::list<T, KHeapAllocator<T>>;

} // namespace dkv::therm
