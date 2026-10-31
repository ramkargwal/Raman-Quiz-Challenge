import React, { useMemo } from 'react';
import { renderMixedLatexToHtml } from '../utils/latexQuizEngine';

interface LatexTextProps {
  text: string;
  className?: string;
  as?: 'span' | 'div';
}

export const LatexText: React.FC<LatexTextProps> = ({
  text,
  className = '',
  as: Tag = 'span',
}) => {
  const html = useMemo(() => renderMixedLatexToHtml(text), [text]);

  return (
    <Tag
      className={`latex-rendered ${className}`}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
};
