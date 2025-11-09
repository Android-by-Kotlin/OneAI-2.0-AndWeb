import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import rehypeRaw from 'rehype-raw';
import { Copy, Check } from 'lucide-react';
import 'highlight.js/styles/atom-one-dark.css';

interface MessageContentProps {
  content: string;
}

const MessageContent: React.FC<MessageContentProps> = ({ content }) => {
  const [copiedCode, setCopiedCode] = React.useState<string | null>(null);

  // Extract text from React children (handles nested elements)
  const extractText = (children: any): string => {
    if (typeof children === 'string') {
      return children;
    }
    if (Array.isArray(children)) {
      return children.map(extractText).join('');
    }
    if (children?.props?.children) {
      return extractText(children.props.children);
    }
    return '';
  };

  const copyToClipboard = (code: string, id: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(id);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  // Detect if content is markdown or has formatting
  const hasMarkdownOrFormatting = (text: string): boolean => {
    // Check for common markdown patterns
    const markdownPatterns = [
      /^#{1,6}\s/m,           // Headers
      /\*\*[^*]+\*\*/,        // Bold (more specific)
      /\*[^*\s][^*]*\*/,      // Italic (excluding single asterisks)
      /```[\s\S]*?```/,       // Code blocks
      /`[^`]+`/,              // Inline code
      /^\s*[-*+]\s/m,         // Unordered lists
      /^\s*\d+\.\s/m,         // Ordered lists
      /\[.+?\]\(.+?\)/,       // Links (more specific)
      /^>\s/m,                // Blockquotes
      /^---+$/m,              // Horizontal rules
      /\n\n/,                 // Multiple line breaks (paragraph separation)
      /\n.*:\n/,              // Text with colons (often used for structure)
    ];
    return markdownPatterns.some(pattern => pattern.test(text));
  };

  // Check if content has any markdown or formatting
  const shouldUseMarkdown = hasMarkdownOrFormatting(content);
  
  // For plain text with single line breaks, preserve formatting with whitespace-pre-wrap
  if (!shouldUseMarkdown && content.includes('\n')) {
    return (
      <div className="text-gray-100 leading-7 break-words whitespace-pre-wrap">
        {content}
      </div>
    );
  }
  
  // For very simple single-line text, render as plain text
  if (!shouldUseMarkdown && !content.includes('\n')) {
    return (
      <div className="text-gray-100 leading-7 break-words">
        {content}
      </div>
    );
  }

  return (
    <div className="prose prose-invert prose-sm max-w-none break-words">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight, rehypeRaw]}
        components={{
          // Custom code block rendering
          code({ node, inline, className, children, ...props }: any) {
            const match = /language-(\w+)/.exec(className || '');
            // Extract plain text from children properly
            const codeText = extractText(children);
            const codeId = `code-${Math.random()}`;

            if (!inline && match) {
              return (
                <div className="relative group my-4">
                  {/* Language label */}
                  <div className="flex items-center justify-between bg-black px-4 py-2 rounded-t-lg border-b border-gray-700">
                    <span className="text-sm font-mono text-gray-400">{match[1]}</span>
                    <button
                      onClick={() => copyToClipboard(codeText, codeId)}
                      className="flex items-center gap-1 text-xs text-gray-400 hover:text-white transition-colors"
                    >
                      {copiedCode === codeId ? (
                        <>
                          <Check className="w-3 h-3" />
                          Copied!
                        </>
                      ) : (
                        <>
                          <Copy className="w-3 h-3" />
                          Copy
                        </>
                      )}
                    </button>
                  </div>
                  {/* Code content */}
                  <pre className="!mt-0 !rounded-t-none !bg-black !p-4 overflow-x-auto">
                    <code className={`${className} !text-base`} {...props}>
                      {children}
                    </code>
                  </pre>
                </div>
              );
            }

            // Inline code
            return (
              <code
                className="bg-gray-800 text-emerald-400 px-1.5 py-0.5 rounded text-base font-mono"
                {...props}
              >
                {children}
              </code>
            );
          },
          // Custom paragraph rendering
          p({ children }) {
            return <p className="mb-3 leading-7 text-gray-100 break-words">{children}</p>;
          },
          // Custom heading rendering
          h1({ children }) {
            return <h1 className="text-2xl font-bold mb-4 mt-6 text-white">{children}</h1>;
          },
          h2({ children }) {
            return <h2 className="text-xl font-bold mb-3 mt-5 text-white">{children}</h2>;
          },
          h3({ children }) {
            return <h3 className="text-lg font-semibold mb-2 mt-4 text-white">{children}</h3>;
          },
          // Custom list rendering
          ul({ children }) {
            return <ul className="list-disc list-inside mb-3 space-y-1.5 text-gray-100 ml-4">{children}</ul>;
          },
          ol({ children }) {
            return <ol className="list-decimal list-inside mb-3 space-y-1.5 text-gray-100 ml-4">{children}</ol>;
          },
          li({ children }) {
            return <li className="leading-7 break-words">{children}</li>;
          },
          // Custom blockquote rendering
          blockquote({ children }) {
            return (
              <blockquote className="border-l-4 border-emerald-500 pl-4 italic my-4 text-gray-300">
                {children}
              </blockquote>
            );
          },
          // Custom table rendering
          table({ children }) {
            return (
              <div className="overflow-x-auto my-4">
                <table className="min-w-full border border-gray-700 rounded-lg">
                  {children}
                </table>
              </div>
            );
          },
          thead({ children }) {
            return <thead className="bg-gray-800">{children}</thead>;
          },
          tbody({ children }) {
            return <tbody className="bg-gray-900">{children}</tbody>;
          },
          tr({ children }) {
            return <tr className="border-b border-gray-700">{children}</tr>;
          },
          th({ children }) {
            return (
              <th className="px-4 py-2 text-left text-sm font-semibold text-white">
                {children}
              </th>
            );
          },
          td({ children }) {
            return (
              <td className="px-4 py-2 text-sm text-gray-100">
                {children}
              </td>
            );
          },
          // Custom link rendering
          a({ href, children }) {
            return (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-emerald-400 hover:text-emerald-300 underline transition-colors"
              >
                {children}
              </a>
            );
          },
          // Custom horizontal rule
          hr() {
            return <hr className="my-6 border-gray-700" />;
          },
          // Strong/bold text
          strong({ children }) {
            return <strong className="font-bold text-white">{children}</strong>;
          },
          // Emphasis/italic text
          em({ children }) {
            return <em className="italic text-gray-200">{children}</em>;
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MessageContent;
