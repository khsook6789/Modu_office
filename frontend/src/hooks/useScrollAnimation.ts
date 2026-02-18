
import { useEffect, useRef, useState } from 'react';

/**
 * Hook to trigger animations when element enters viewport
 * Example usage:
 * const { ref, isVisible } = useScrollAnimation();
 * <div ref={ref} className={isVisible ? 'animate-fade-in-up' : 'opacity-0'}>...</div>
 */
export function useScrollAnimation(threshold = 0.1) {
  const ref = useRef<HTMLDivElement | null>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        // Once visible, keep it visible (don't fade out when scrolling up)
        if (entry.isIntersecting) {
          setIsVisible(true);
          observer.unobserve(entry.target);
        }
      },
      {
        threshold,
        rootMargin: '0px 0px -50px 0px', // Trigger slightly before element is fully in view
      }
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [threshold]);

  return { ref, isVisible };
}
